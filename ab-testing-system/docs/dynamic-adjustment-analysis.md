# A/B Testing - 动态调整策略对比

## 📊 三种实现方案

### 方案1：固定阈值（当前实现）✅

**原理**：
```
阈值固定：[10, 30, 60, 100]
基于用户ID hash分配，不调整
```

**优点**：
- ✅ 100%用户一致性
- ✅ 性能最高（O(1)）
- ✅ 无状态，分布式友好
- ✅ 实现简单

**缺点**：
- ⚠️ 分组比例可能有偏差（但会随用户增加自动收敛）
- ⚠️ 无法人工干预调整

**适用场景**：
- ✅ 用户多次访问（你的核心需求）
- ✅ 大规模用户（偏差会自动收敛）
- ✅ 分布式系统

**一致性**：**100%** ⭐⭐⭐⭐⭐

---

### 方案2：完全动态调整 ❌

**原理**：
```
每次分配时，根据实际偏差动态调整阈值
选择偏差最大的组
```

**这就是Greedy算法**

**优点**：
- ✅ 分组比例完美（MSE = 0）

**缺点**：
- ❌ **0%用户一致性**（致命）
- ❌ 需要维护状态
- ❌ 分布式复杂

**适用场景**：
- ❌ 不适用于你的场景

**一致性**：**0%** ❌

---

### 方案3：部分动态调整（推荐）⭐

**原理**：
```
1. 老用户：保持原分组（一致性优先）
2. 新用户：根据当前偏差动态调整
```

**实现逻辑**：
```java
// 伪代码
String userId = user.getUserId();
String previousGroup = userGroupMapping.get(userId);

if (previousGroup != null) {
    // 老用户：保持原分组
    return previousGroup;
} else {
    // 新用户：根据当前偏差动态分配
    return selectGroupByDeviation();
}
```

**优点**：
- ✅ 老用户100%一致性
- ✅ 可以微调分组比例
- ✅ 性能高

**缺点**：
- ⚠️ 需要存储用户分组映射（Redis）
- ⚠️ 新用户调整幅度有限

**适用场景**：
- ✅ 用户多次访问
- ✅ 需要精确控制分组比例
- ✅ 可以接受存储成本

**一致性**：**老用户100%，新用户动态调整** ⭐⭐⭐⭐⭐

---

## 🎯 推荐方案对比

| 指标 | 固定阈值 | 完全动态 | 部分动态 |
|------|----------|----------|----------|
| **用户一致性** | 100% ✅ | 0% ❌ | 老用户100% ✅ |
| **分组精度** | 优秀 | 完美 | 优秀 |
| **性能** | 最高 | 中等 | 高 |
| **存储成本** | 无 | 有 | 有 |
| **分布式友好** | 是 | 否 | 是（Redis） |
| **实现复杂度** | 简单 | 中等 | 中等 |

---

## 💡 你的业务场景分析

### 场景1：日常活动（低QPS）

**特点**：
- 用户多次访问
- 分组一致性是核心需求
- 用户量：数万到千万级

**推荐**：**方案1（固定阈值）** 或 **方案3（部分动态）**

**理由**：
- 方案1：简单可靠，偏差会随用户增加自动收敛
- 方案3：如果需要更精确控制，可以存储用户分组

### 场景2：抢购活动（高QPS）

**特点**：
- QPS > 1000
- 需要快速响应
- 用户可能多次进入

**推荐**：**方案1（固定阈值）**

**理由**：
- 无状态，性能最高
- 支持分布式扩展
- 不需要存储

---

## 🔧 方案3实现示例

如果你需要方案3（部分动态调整），我可以帮你实现：

```java
@Service
public class HybridAssignmentService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    // 用户分组映射（Redis）
    private static final String USER_GROUP_KEY = "ab:user:group:";
    
    public AssignmentResult assign(User user, Experiment experiment) {
        String userId = user.getUserId();
        
        // 1. 检查是否已有分组
        String cachedGroup = redisTemplate.opsForValue()
            .get(USER_GROUP_KEY + userId);
        
        if (cachedGroup != null) {
            // 老用户：保持原分组
            return createResult(user, cachedGroup, experiment);
        }
        
        // 2. 新用户：根据当前偏差动态分配
        String selectedGroup = selectGroupByDeviation(experiment);
        
        // 3. 缓存分组结果（30天过期）
        redisTemplate.opsForValue()
            .set(USER_GROUP_KEY + userId, selectedGroup, 30, TimeUnit.DAYS);
        
        return createResult(user, selectedGroup, experiment);
    }
    
    private String selectGroupByDeviation(Experiment experiment) {
        // 获取当前统计
        Map<String, Double> actualRatios = getCurrentRatios();
        
        // 计算每个组的偏差
        double maxDeviation = Double.NEGATIVE_INFINITY;
        String selectedGroup = "A";
        
        for (Group group : experiment.getGroups()) {
            double target = group.getTargetRatio();
            double actual = actualRatios.getOrDefault(group.getName(), 0.0);
            double deviation = target - actual;
            
            if (deviation > maxDeviation) {
                maxDeviation = deviation;
                selectedGroup = group.getName();
            }
        }
        
        return selectedGroup;
    }
}
```

**优点**：
- 老用户100%一致性
- 新用户可以微调分组比例
- 使用Redis存储，分布式友好

**缺点**：
- 需要Redis（增加依赖）
- 每次查询需要访问Redis（性能略低，但通常 < 1ms）

---

## 📊 性能对比

| 方案 | 单次分配耗时 | 存储成本 | 一致性 |
|------|--------------|----------|--------|
| 固定阈值 | **0.001ms** | 0 | 100% |
| 完全动态 | 0.007ms | 中等 | 0% |
| 部分动态（Redis） | **0.5-1ms** | 高 | 老用户100% |

**结论**：
- 固定阈值：性能最高，一致性最好
- 部分动态：性能略低，但仍然很快（< 1ms），一致性也很好

---

## 🎯 最终建议

### 推荐方案1：**固定阈值（当前实现）**

**适用场景**：
- ✅ 你的两种场景都适用
- ✅ 用户量足够大（数万到千万级）
- ✅ 分组偏差 < 1.5%可接受

**理由**：
1. 100%一致性
2. 性能最高（P999 < 0.01ms）
3. 无需额外存储
4. 实现简单，不易出错
5. 分组偏差会随用户增加自动收敛

### 可选方案3：**部分动态调整**

**适用场景**：
- ✅ 需要精确控制分组比例（偏差 < 0.5%）
- ✅ 可以接受Redis存储成本

**理由**：
1. 老用户100%一致性
2. 新用户可以微调
3. 性能仍然很快（< 1ms）

---

## ❓ 你的选择

**问题1**：你能接受分组偏差 < 1.5%吗？
- 如果**能接受** → 使用方案1（固定阈值）
- 如果**不能接受** → 使用方案3（部分动态）

**问题2**：你有Redis或类似的存储吗？
- 如果**有** → 可以考虑方案3
- 如果**没有** → 使用方案1

**问题3**：你的用户量级？
- 如果**> 10万** → 方案1的偏差会自动收敛到 < 1%
- 如果**< 1万** → 可能需要方案3来精确控制

---

## 📝 我的推荐

**基于你的场景（数万到千万级用户），我强烈推荐方案1（固定阈值）**

**理由**：
1. 100%一致性 - 你的核心需求
2. 性能最高 - P999 < 0.01ms（远超50ms要求）
3. 简单可靠 - 无需额外存储
4. 偏差可接受 - 随用户增加会自动收敛

**如果未来需要更精确控制，可以升级到方案3（部分动态调整）**

---

**你的选择？**
- A. 继续使用方案1（固定阈值）
- B. 升级到方案3（部分动态调整）
- C. 两种方案都实现，让我选择
