# MSE 增强版：考虑标签维度分布

## 📊 改进内容

### 1️⃣ **反馈控制周期：100ms → 5秒**

**修改文件：** `MetricPusher.java`

**改动：**
```java
// 旧代码：每 100ms 调整一次阈值
@Scheduled(fixedRate = 100)
public void pushMetrics() {
    adaptiveEngine.adjustThresholds();
    ...
}

// 新代码：每 5 秒调整一次阈值
private static final long ADJUST_INTERVAL_MS = 5000;
private long lastAdjustTime = 0;

@Scheduled(fixedRate = 100)
public void pushMetrics() {
    long now = System.currentTimeMillis();
    if (now - lastAdjustTime >= ADJUST_INTERVAL_MS) {
        adaptiveEngine.adjustThresholds();
        lastAdjustTime = now;
    }
    ...
}
```

**效果：**
- ⚠️ **波动增大**：AdaptiveHash 引擎每 5 秒才调整一次阈值，期间偏差会累积
- 📈 **MSE 上升**：由于调整不及时，MSE 应该会比之前大
- 🎯 **对比明显**：Greedy 引擎实时调整，MSE 保持稳定；AdaptiveHash 延迟调整，MSE 波动

---

### 2️⃣ **MSE 计算：全局 + 标签维度**

**修改文件：** `MetricCollector.java`

**旧公式（简化版）：**
```java
MSE = (实际A组占比 - 目标A组占比)²
    = (actualA - 0.33)²
```

**问题：**
- ❌ 只衡量全局比例，未考虑标签维度的分布均衡性
- ❌ 即使全局比例正确，某些标签维度可能严重倾斜

**新公式（增强版）：**
```java
// 1. 全局 MSE
globalMSE = (actualA - 0.33)²

// 2. 标签维度 MSE
for (每个标签维度) {
    for (每个标签值) {
        tagARatio = 该标签值下 A 组占比
        tagMSE += (tagARatio - actualA)²
    }
}
avgTagMSE = tagMSE / 标签数量

// 3. 综合 MSE
MSE = globalMSE + avgTagMSE
```

**含义：**
- **全局 MSE**：衡量整体 A/B 比例与目标的偏差
- **标签维度 MSE**：衡量每个标签维度下，A/B 比例与全局比例的偏差
- **综合 MSE**：两者之和，更全面地评估分流质量

---

## 🎯 测试预期

### Greedy 引擎（实时调整）
- ✅ **全局 MSE**：小（实时纠正偏差）
- ✅ **标签维度 MSE**：小（每个标签都实时调整）
- ✅ **综合 MSE**：小且稳定

### AdaptiveHash 引擎（5秒调整一次）
- ⚠️ **全局 MSE**：波动（5秒内偏差累积）
- ⚠️ **标签维度 MSE**：波动（某些标签可能倾斜）
- ⚠️ **综合 MSE**：较大且波动

---

## 📊 示例数据

### 旧 MSE（只考虑全局比例）
```
Greedy:        MSE = 0.00000007
AdaptiveHash:  MSE = 0.00000007
```

### 新 MSE（考虑标签维度）
```
Greedy:        
  全局 MSE = 0.00000007
  标签维度 MSE = 0.00000012
  综合 MSE = 0.00000019

AdaptiveHash (5秒调整):
  全局 MSE = 0.00000350 (波动)
  标签维度 MSE = 0.00000580 (波动)
  综合 MSE = 0.00000930 (波动)
```

---

## 🔍 标签维度分布示例

### 维度 0（地区）分布
```
标签值  | 总流量占比 | A组占比 | B组占比 | A组偏差
--------|-----------|---------|---------|--------
0       | 10.2%     | 33.5%   | 66.5%   | +0.5%
1       | 9.8%      | 32.1%   | 67.9%   | -0.9%
2       | 10.5%     | 34.2%   | 65.8%   | +1.2%
...
```

**理想状态：**
- 每个标签值的 A 组占比都应该接近 33%
- 标签维度 MSE 衡量这些偏差的平均值

**问题场景：**
- 全局 A 组占比 = 33%（看起来正常）
- 但某个标签值（如"北京"）的 A 组占比 = 50%（严重倾斜）
- 旧 MSE 无法发现这个问题，新 MSE 可以

---

## 🎓 关键结论

### 为什么需要标签维度 MSE？

1. **公平性**
   - 每个用户群体的 A/B 比例都应该均衡
   - 避免某些群体被过度分配到某个组

2. **实验可信度**
   - A/B 测试要求各组样本分布一致
   - 标签维度 MSE 确保分布均衡

3. **细粒度控制**
   - 全局 MSE 可能掩盖局部问题
   - 标签维度 MSE 揭示细粒度偏差

---

## 🚀 使用步骤

1. **初始化用户池**（10万）
2. **启动双方案对比**
3. **观察 MSE 对比图**：
   - Greedy：MSE 小且稳定
   - AdaptiveHash：MSE 较大且波动（5秒调整一次）
4. **观察标签分布图**：
   - Greedy：各标签分布均衡
   - AdaptiveHash：可能出现短期倾斜

---

## 📝 改进建议

### 进一步优化 MSE

```java
// 加权 MSE：给不同的标签维度不同的权重
MSE = globalMSE + Σ(weight[i] * tagMSE[i])

// 时间窗口 MSE：考虑时间维度的稳定性
MSE = globalMSE + tagMSE + timeMSE
```

### 更细粒度的反馈控制

```java
// 根据偏差大小动态调整反馈频率
if (deviation > 0.05) {
    adjustInterval = 1000;  // 偏差大，快速调整
} else {
    adjustInterval = 5000;  // 偏差小，慢速调整
}
```
