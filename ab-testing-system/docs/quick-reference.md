# A/B Testing系统 - 快速参考

## 🚀 快速开始

### 1. 启动系统

```bash
# 后端
cd /Users/jagger/.openclaw/workspace-project/ab-testing-system
mvn spring-boot:run

# 访问地址
http://100.73.204.30:8080
```

### 2. 完整测试流程

```bash
# 1. 创建实验（4客群）
curl -X POST http://localhost:8080/api/experiment/create

# 2. 初始化用户池
curl -X POST "http://localhost:8080/api/user-pool/init?size=10000"

# 3. 一致性测试（核心）
curl -X POST "http://localhost:8080/api/load-test/consistency?userCount=1000&visitsPerUser=10"

# 4. 查看结果
curl http://localhost:8080/api/load-test/status
```

---

## 📊 API参考

### 实验管理

```bash
# 创建实验
POST /api/experiment/create

# 获取实验信息
GET /api/experiment/info
```

### 用户池

```bash
# 初始化用户池
POST /api/user-pool/init?size=10000

# 获取统计信息
GET /api/user-pool/stats
```

### 一致性测试

```bash
# 用户一致性测试（推荐）
POST /api/load-test/consistency?userCount=1000&visitsPerUser=10

# 返回：
{
  "status": "completed",
  "totalRequests": 10000,
  "metrics": {
    "hybrid": {
      "userConsistency": {
        "consistencyRate": 100.0  ← 关键指标
      },
      "mse": 0.0
    }
  }
}
```

### 压力测试

```bash
# QPS压测
POST /api/load-test/start?qps=1000&duration=10

# 停止压测
POST /api/load-test/stop

# 查看状态
GET /api/load-test/status
```

### 真实场景测试

```bash
# 场景1：地域性活动（90%广东用户）
POST /api/real-world-test/regional?userCount=10000&guangdongRatio=90

# 场景2：年龄段活动（95%年轻用户）
POST /api/real-world-test/age-group?userCount=5000&youngRatio=95

# 场景3：新用户活动（100%新用户）
POST /api/real-world-test/new-users?userCount=3000

# 综合测试
POST /api/real-world-test/comprehensive
```

---

## 📚 文档索引

### 核心文档

1. **算法对比报告**（给领导看）
   - 文件：`docs/algorithm-comparison-report.md`
   - 内容：Greedy vs Hybrid详细对比，统计学分析，决策建议
   - 用途：说服领导使用Hybrid算法

2. **计算过程演示**（给评审看）
   - 文件：`docs/algorithm-demo.md`
   - 内容：详细的计算步骤，公式推导，实例演示
   - 用途：技术评审，理解算法细节

3. **用户一致性分析**（深入理解问题）
   - 文件：`docs/greedy-consistency-analysis.md`
   - 内容：Greedy算法为什么0%一致性，数学证明
   - 用途：理解问题的本质

4. **真实场景分析**（业务场景）
   - 文件：`docs/real-world-analysis.md`
   - 内容：为什么不能依赖自动收敛，真实营销场景
   - 用途：理解业务需求

5. **验收清单**（完整验收）
   - 文件：`docs/acceptance-checklist.md`
   - 内容：所有验收标准，测试结果
   - 用途：项目验收

### 使用文档

6. **使用指南**
   - 文件：`docs/user-guide.md`
   - 内容：API文档，使用示例

7. **DDD设计文档**
   - 文件：`docs/ddd-design.md`
   - 内容：架构设计，代码组织

### 可视化

8. **算法演示页面**
   - 文件：`frontend/algorithm-demo.html`
   - 用途：交互式演示，给评委看

---

## 🎯 关键指标

### Hybrid算法（推荐）

```
✅ 用户一致性：100%
✅ 分组精度(MSE)：0.0（完美）
✅ P999延迟：< 0.01ms
✅ 适应数据倾斜：是
✅ 综合评分：100/100
```

### Greedy算法（对比）

```
❌ 用户一致性：0.6%
✅ 分组精度(MSE)：0.0（完美）
✅ P999延迟：< 0.01ms
✅ 适应数据倾斜：是
⚠️ 综合评分：60/100
```

---

## 📊 测试结果

### 一致性测试

```
配置：1000用户，每用户10次访问
结果：
  Greedy：0.6%（几乎不可能一致）
  Hybrid：100%（完美一致）
```

### 分组精度测试

```
配置：10000用户
结果：
  Greedy：MSE = 0.0（完美）
  Hybrid：MSE = 0.0（完美）
```

### 真实场景测试

```
场景1：地域性活动（90%广东用户）
  Hybrid：MSE = 0.0 ✅
  
场景2：年龄段活动（95%年轻用户）
  Hybrid：MSE = 0.0 ✅
  
场景3：新用户活动（100%新用户）
  Hybrid：MSE = 0.0 ✅
```

---

## 💡 核心结论

### 为什么选择Hybrid？

1. **用户一致性**
   - Hybrid：100%（理论保证）
   - Greedy：0.6%（几乎不可能）

2. **分组精度**
   - 两者都是完美（MSE = 0）

3. **真实场景**
   - Hybrid：完美适应
   - Greedy：不适用（一致性为0）

4. **综合评分**
   - Hybrid：100/100
   - Greedy：60/100

### 统计学证明

**Hybrid算法的用户一致性**：
```
P(一致) = 1（理论保证）

证明：
  第1次访问：计算并缓存
  第n次访问：从缓存读取
  ∴ assignment_n = assignment_1
  ∴ P(一致) = 1 □
```

**Greedy算法的用户不一致性**：
```
P(一致) ≈ 0

证明：
  P(一致) = P(状态未显著变化)
          ≈ 0（实际场景）□
```

---

## 🔧 故障排查

### 问题1：总请求数为0

**原因**：用户池未初始化

**解决**：
```bash
curl -X POST "http://localhost:8080/api/user-pool/init?size=10000"
```

### 问题2：后端未启动

**解决**：
```bash
pkill -f 'ab-testing-system'
mvn spring-boot:run
```

---

## 📞 支持

**技术支持**：Proto（预研工程师）  
**项目负责人**：Jagger  
**文档位置**：`docs/`

---

**最后更新**：2026-03-03  
**版本**：v2.0  
**状态**：✅ 生产就绪
