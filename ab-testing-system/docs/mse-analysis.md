# MSE 计算逻辑详解

## 📊 MSE 定义

**MSE（Mean Squared Error，均方误差）** 衡量的是实际分布与目标分布的偏差。

---

## 🧮 当前实现

### 简化版 MSE

```java
// 计算 MSE（简化版：基于组比例偏差）
double mse = 0.0;
if (total > 0) {
    double targetA = 0.33;  // 目标：A组占 33%
    double actualA = groupRatios.get("A");  // 实际：A组当前占比
    mse = Math.pow(actualA - targetA, 2);  // 平方差
}
```

### 计算公式

```
MSE = (实际A组占比 - 目标A组占比)²
    = (actualA - 0.33)²
```

---

## 🎯 为什么 MSE 很小？

### 1️⃣ **Greedy 引擎**

```java
// 贪心决策：优先分配给占比低于目标的组
if (total == 0) {
    group = Math.random() < TARGET_A_RATIO ? "A" : "B";
} else {
    double currentARatio = (double) countA / total;
    group = currentARatio < TARGET_A_RATIO ? "A" : "B";
}
```

**工作原理：**
- 实时监控 A 组占比
- 如果 A 组占比 < 33%，分配到 A 组
- 如果 A 组占比 ≥ 33%，分配到 B 组
- **结果**：A 组占比始终在 33% 附近波动

**示例：**
```
请求1: A组0%, B组0% → 分配到A (随机)
请求2: A组100%, B组0% → 分配到B
请求3: A组50%, B组50% → 分配到B
请求4: A组33%, B组67% → 分配到B
请求5: A组25%, B组75% → 分配到A
...
最终：A组占比 ≈ 33%
```

---

### 2️⃣ **AdaptiveHash 引擎**

```java
// 哈希 + 阈值分流
double threshold = thresholdMap.getOrDefault(comboKey, INITIAL_THRESHOLD);  // 初始 33.0
int hash = hashUserId(user.userId());
String group = (Math.abs(hash) % 100) < threshold ? "A" : "B";

// 后台每100ms调整阈值
public void adjustThresholds() {
    double currentARatio = (double) countA / total;
    double deviation = currentARatio - 0.33;
    
    // PI控制器调整阈值
    double adjustment = deviation * KP;
    double newThreshold = 33.0 - adjustment * 100;
    thresholdMap.put(comboKey, Math.max(0, Math.min(100, newThreshold)));
}
```

**工作原理：**
- 使用哈希值 % 100 与阈值比较
- 初始阈值 = 33（期望33%到A组）
- 每100ms根据偏差调整阈值
- **结果**：通过反馈控制，A组占比趋向33%

**示例：**
```
初始阈值: 33.0
第1次调整: A组占比40% → 偏差+7% → 降低阈值到 26.0
第2次调整: A组占比28% → 偏差-5% → 提高阈值到 31.0
第3次调整: A组占比34% → 偏差+1% → 微调阈值到 32.0
...
最终：A组占比 ≈ 33%
```

---

## 📈 MSE 对比

### 理论 MSE

如果完全随机分配（A组概率33%）：
- 样本数 n = 1000
- 方差 Var = p(1-p)/n = 0.33 * 0.67 / 1000 = 0.000221
- 标准差 σ = √Var ≈ 0.0149 (1.49%)
- **期望 MSE ≈ 0.000221**

### 实际 MSE

| 引擎 | MSE | A组占比 | 偏差 |
|------|-----|---------|------|
| Greedy | 0.00000007 | 32.97% | -0.03% |
| AdaptiveHash | 0.00000007 | 32.97% | -0.03% |

**偏差 = |实际占比 - 目标占比| = |32.97% - 33%| = 0.03%**

---

## 🔍 为什么效果这么好？

### Greedy 引擎的优势

1. **强一致性**
   - 每次请求都检查当前比例
   - 立即纠正偏差
   - 不会累积误差

2. **贪心策略**
   - 始终优先分配给占比低的组
   - 自动平衡流量

3. **示例轨迹**
   ```
   A组: 0% → 50% → 33% → 25% → 33% → 33% → 33% ...
   ```

### AdaptiveHash 引擎的优势

1. **哈希稳定性**
   - 相同用户ID总是得到相同的哈希值
   - 保证用户分组一致性

2. **反馈控制**
   - 每100ms检测偏差
   - 动态调整阈值
   - 快速收敛到目标

3. **示例轨迹**
   ```
   阈值: 33 → 26 → 31 → 32 → 33 → 33 → 33 ...
   A组: 40% → 28% → 34% → 32% → 33% → 33% → 33% ...
   ```

---

## 🎓 关键结论

### 为什么 MSE 都很小？

1. **两个引擎都设计为将 A 组占比控制在 33% 附近**
2. **MSE 只衡量 A 组占比与目标的偏差**
3. **样本量足够大时，统计规律保证稳定性**

### 两个引擎的区别

| 维度 | Greedy | AdaptiveHash |
|------|--------|--------------|
| **一致性** | 强一致性 | 最终一致性 |
| **延迟** | 需要加锁，P99 较高 | 无锁读取，P99 较低 |
| **稳定性** | 实时调整 | 周期性调整 |
| **用户粘性** | ❌ 不保证 | ✅ 哈希保证 |

### MSE 的局限性

当前 MSE 只衡量**全局 A/B 比例**，未考虑：

1. **标签维度的分布均衡**
   - 例如：不同地区、年龄段的 A/B 比例
   
2. **时间维度的稳定性**
   - 例如：每分钟的 A/B 比例波动

3. **用户粘性**
   - 例如：同一用户多次访问是否分到同一组

---

## 💡 改进建议

### 更全面的 MSE

```java
// 当前：只衡量全局比例
mse = Math.pow(actualA - targetA, 2);

// 改进：衡量标签维度的分布均衡
double mse = 0.0;
int dimensionCount = 0;

for (int dim = 0; dim < 5; dim++) {
    for (int tagValue : tagValues[dim]) {
        double dimRatioA = getGroupRatioInDimension(dim, tagValue, "A");
        double globalRatioA = getGlobalRatio("A");
        
        // 该标签维度下，A组占比与全局占比的偏差
        mse += Math.pow(dimRatioA - globalRatioA, 2);
        dimensionCount++;
    }
}

mse /= dimensionCount;  // 平均 MSE
```

---

## 📊 总结

**当前 MSE 很小的原因：**
1. 两个引擎都能有效控制 A 组占比接近 33%
2. MSE 只衡量全局比例，未考虑细粒度分布
3. 样本量大时，统计规律保证稳定性

**如果要更全面评估分流质量，需要：**
1. 考虑标签维度的分布均衡性
2. 考虑时间维度的稳定性
3. 考虑用户分组的一致性
