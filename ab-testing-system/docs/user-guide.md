# A/B Testing 4客群系统 - 使用指南

## 🚀 快速开始

### 1. 启动系统

```bash
# 启动后端
cd /Users/jagger/.openclaw/workspace-project/ab-testing-system
mvn spring-boot:run

# 或者使用jar包
java -jar target/ab-testing-system-1.0.0-SNAPSHOT.jar
```

### 2. 基本使用流程

```bash
# 1. 创建实验（4客群：A:10%, B:20%, C:30%, D:40%）
curl -X POST http://localhost:8080/api/experiment/create

# 2. 初始化用户池
curl -X POST "http://localhost:8080/api/user-pool/init?size=10000"

# 3. 运行一致性测试（推荐）
curl -X POST "http://localhost:8080/api/load-test/consistency?userCount=1000&visitsPerUser=10"

# 4. 查看结果
curl http://localhost:8080/api/load-test/status
```

---

## 📊 API接口文档

### 实验管理

#### 创建实验
```bash
POST /api/experiment/create

响应：
{
  "experimentId": "exp-001",
  "experimentName": "四客群实验",
  "groupCount": 4,
  "groups": [...],
  "tagDimensions": [...],
  "totalCombinations": 18
}
```

#### 获取实验信息
```bash
GET /api/experiment/info
```

### 用户池管理

#### 初始化用户池
```bash
POST /api/user-pool/init?size=10000

响应：
{
  "totalUsers": 10000,
  "tagDistribution": {...},
  "userExamples": [...]
}
```

#### 获取用户池统计
```bash
GET /api/user-pool/stats
```

### 压力测试

#### 一致性测试（推荐）
```bash
POST /api/load-test/consistency?userCount=1000&visitsPerUser=10

参数：
- userCount: 用户数量（默认1000）
- visitsPerUser: 每个用户访问次数（默认10）

响应：
{
  "status": "completed",
  "userCount": 1000,
  "visitsPerUser": 10,
  "totalRequests": 10000,
  "elapsedMs": 13,
  "metrics": {
    "greedy": {...},
    "adaptiveHash": {
      "userConsistency": {
        "consistencyRate": 100.0  // 一致性比率
      }
    },
    "comparison": {
      "overall": {
        "recommendation": "AdaptiveHash（推荐）"
      }
    }
  }
}
```

#### QPS压力测试
```bash
POST /api/load-test/start?qps=100&duration=10

参数：
- qps: 每秒请求数（默认100）
- duration: 持续时间/秒（默认10）

响应：
{
  "status": "started",
  "qps": 100,
  "duration": 10,
  "totalRequests": 1000
}
```

#### 停止压测
```bash
POST /api/load-test/stop
```

#### 获取压测状态
```bash
GET /api/load-test/status

响应：
{
  "running": false,
  "totalRequests": 10000,
  "experimentStatus": "STOPPED",
  "metrics": {
    "greedy": {
      "engine": "Greedy",
      "totalRequests": 10000,
      "latency": {
        "p50": 0.0,
        "p90": 0.0,
        "p95": 0.001,
        "p99": 0.001,
        "p999": 0.007,
        "avg": 0.0001164,
        "max": 0.41
      },
      "groups": {...},
      "mse": 0.0
    },
    "adaptiveHash": {
      "engine": "AdaptiveHash",
      "totalRequests": 10000,
      "latency": {...},
      "groups": {...},
      "mse": 0.000264,
      "userConsistency": {
        "totalRevisits": 9000,
        "consistentCount": 9000,
        "inconsistentCount": 0,
        "consistencyRate": 100.0
      }
    },
    "comparison": {
      "accuracy": {...},
      "performance": {...},
      "consistency": {...},
      "overall": {
        "greedyScore": 59.9979,
        "hashScore": 99.2068,
        "recommendation": "AdaptiveHash（推荐）"
      }
    }
  }
}
```

---

## 🎯 关键指标说明

### 1. 用户一致性（Consistency）

**定义**：同一用户多次访问，是否分到同一组

**重要性**：⭐⭐⭐⭐⭐（核心指标）

**测试方法**：
```bash
curl -X POST "http://localhost:8080/api/load-test/consistency?userCount=1000&visitsPerUser=10"
```

**预期结果**：
- AdaptiveHash: **100%** ✅
- Greedy: **0%** ❌

### 2. 分组精度（Accuracy）

**定义**：实际分组比例与目标比例的偏差

**重要性**：⭐⭐⭐⭐

**指标**：
- MSE（均方误差）：越小越好
- 偏差百分比：< 5%可接受，< 1%优秀

**预期结果**：
- Greedy: MSE < 0.0001（完美）
- AdaptiveHash: MSE < 0.001（优秀）

### 3. 响应时间（Latency）

**定义**：单次分配的耗时

**重要性**：⭐⭐⭐⭐⭐

**关键指标**：
- P50: 50%的请求延迟
- P90: 90%的请求延迟
- P95: 95%的请求延迟
- P99: 99%的请求延迟
- **P999: 99.9%的请求延迟**（最重要）

**要求**：
- P999 < 50ms（你的要求）
- 预期：P999 < 0.01ms（远超要求）

---

## 📈 测试场景

### 场景1：日常活动（低QPS）

**特点**：
- QPS < 100
- 用户多次访问
- 需要分组一致

**测试命令**：
```bash
# 初始化
curl -X POST http://localhost:8080/api/experiment/create
curl -X POST "http://localhost:8080/api/user-pool/init?size=10000"

# 一致性测试
curl -X POST "http://localhost:8080/api/load-test/consistency?userCount=1000&visitsPerUser=10"

# 低QPS压测
curl -X POST "http://localhost:8080/api/load-test/start?qps=10&duration=30"
sleep 32
curl http://localhost:8080/api/load-test/status
```

### 场景2：抢购活动（高QPS）

**特点**：
- QPS > 1000
- 需要快速稳定响应
- 需要分组一致

**测试命令**：
```bash
# 初始化
curl -X POST http://localhost:8080/api/experiment/create
curl -X POST "http://localhost:8080/api/user-pool/init?size=50000"

# 高QPS压测
curl -X POST "http://localhost:8080/api/load-test/start?qps=1000&duration=10"
sleep 12
curl http://localhost:8080/api/load-test/status
```

---

## 🏆 算法选择建议

### 推荐算法：AdaptiveHash

**适用场景**：
- ✅ 日常活动（低QPS）
- ✅ 抢购活动（高QPS）
- ✅ 需要用户一致性

**优点**：
- 100%用户一致性
- 性能稳定（P999 < 0.01ms）
- 无状态，分布式友好
- 分组精度优秀（偏差 < 1.5%）

### 不推荐：Greedy

**仅适用场景**：
- 一次性访问（用户不会重复）
- 对分组精度要求极高（偏差必须 < 0.1%）

**缺点**：
- 不支持用户一致性
- 需要维护状态
- 分布式复杂

---

## 🎨 可视化建议

### 1. 实时仪表盘

显示内容：
- 4个客群的实时流量
- 分组比例偏差
- P999延迟
- 用户一致性

### 2. 算法对比雷达图

维度：
- 分组精度（30%）
- 性能稳定性（30%）
- 用户一致性（40%）

### 3. 延迟分布直方图

显示：
- P50, P90, P95, P99, P999
- 最大延迟

### 4. 用户一致性热力图

显示：
- 同一用户多次访问的分组结果
- 验证一致性

---

## 📝 注意事项

1. **测试前必须初始化**
   - 先创建实验
   - 再初始化用户池
   - 最后运行测试

2. **一致性测试最关键**
   - 这是你的核心需求
   - AdaptiveHash保证100%一致性

3. **性能远超要求**
   - P999 < 0.01ms（远低于50ms要求）
   - 支持高QPS（1000+）

4. **分组偏差可接受**
   - AdaptiveHash偏差 < 1.5%
   - 随着用户增加会自动收敛

---

## 🔧 故障排查

### 问题1：总请求数为0

**原因**：用户池未初始化

**解决**：
```bash
curl -X POST "http://localhost:8080/api/user-pool/init?size=10000"
```

### 问题2：压测未启动

**原因**：实验未创建

**解决**：
```bash
curl -X POST http://localhost:8080/api/experiment/create
```

### 问题3：一致性比率异常

**原因**：测试样本太少

**解决**：增加用户数量和访问次数
```bash
curl -X POST "http://localhost:8080/api/load-test/consistency?userCount=5000&visitsPerUser=20"
```

---

## 📞 联系方式

- 开发者：Proto（预研机器人）
- 测试人：Jagger
- 文档位置：`/docs/`

---

**最后更新**: 2026-03-03
