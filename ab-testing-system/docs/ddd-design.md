# A/B Testing 4客群系统 - DDD设计文档

## 📊 系统配置

### 客群配置
- **客群数量**: 4个（A, B, C, D）
- **流量比例**: 10% : 20% : 30% : 40%

### 标签配置（简化版）
为了便于验证，减少标签复杂度：

#### 1. 性别（2个枚举）
- 男: 55%
- 女: 45%

#### 2. 省份（3个枚举）
- 广东: 40%
- 山东: 35%
- 江苏: 25%

#### 3. 年龄段（3个枚举）
- 18-25岁: 40%
- 26-35岁: 35%
- 36-45岁: 25%

**总组合数**: 2 × 3 × 3 = **18种**

## 🏗️ DDD设计

### 领域模型（Domain Model）

```
com.example.abtesting.domain
├── model/                    # 领域模型
│   ├── Experiment.java       # 实验聚合根
│   ├── Group.java           # 分组实体
│   ├── User.java            # 用户实体
│   ├── TagSchema.java       # 标签维度定义（值对象）
│   └── TagDimension.java    # 标签维度（值对象）
├── valueobject/             # 值对象
│   └── AssignmentResult.java # 分配结果
├── service/                 # 领域服务
│   ├── AssignmentService.java  # 分配服务接口
│   ├── GreedyAssignmentService.java  # 贪心分配
│   └── AdaptiveHashAssignmentService.java  # 自适应哈希
└── factory/                 # 工厂
    └── ExperimentFactory.java  # 实验工厂
```

### 分层架构

```
┌─────────────────────────────────────┐
│  Presentation Layer (Controller)    │  REST API / WebSocket
├─────────────────────────────────────┤
│  Application Layer (Service)        │  实验应用服务
├─────────────────────────────────────┤
│  Domain Layer (Model + Service)     │  核心业务逻辑
├─────────────────────────────────────┤
│  Infrastructure Layer (Config)      │  配置、持久化
└─────────────────────────────────────┘
```

## 🎯 核心设计原则

### 1. 聚合根（Aggregate Root）
- `Experiment` 是聚合根
- 包含多个 `Group` 实体
- 包含 `TagSchema` 值对象
- 控制所有内部对象的访问

### 2. 值对象（Value Object）
- `TagSchema`, `TagDimension`, `AssignmentResult` 是值对象
- 不可变（Immutable）
- 通过ID比较，而不是引用

### 3. 实体（Entity）
- `User`, `Group` 是实体
- 有唯一标识
- 可变（状态可改变）

### 4. 领域服务（Domain Service）
- `AssignmentService` 是领域服务
- 封装跨聚合的业务逻辑
- 不属于任何实体或值对象

### 5. 工厂（Factory）
- `ExperimentFactory` 负责创建实验聚合
- 封装复杂的创建逻辑
- 确保创建的对象处于有效状态

## 🔄 工作流程

1. **初始化**
   ```
   ExperimentFactory.createDefaultExperiment()
   → 创建4个Group
   → 创建TagSchema（3个TagDimension）
   → 创建Experiment聚合根
   ```

2. **用户生成**
   ```
   根据TagSchema的分布比例
   → 为每个维度选择枚举值
   → 创建User实体
   ```

3. **流量分配**
   ```
   AssignmentService.assign(User, Experiment)
   → 根据算法（Greedy/AdaptiveHash）
   → 返回AssignmentResult
   ```

4. **指标收集**
   ```
   MetricCollector.record(AssignmentResult)
   → 更新分组计数
   → 更新标签分布
   → 计算MSE
   ```

## 📈 验证步骤

### 第一步：验证领域模型
```bash
# 测试实验创建
curl -X POST http://localhost:8080/api/experiment/create

# 预期：返回实验信息，包含4个分组和18种组合
```

### 第二步：验证用户生成
```bash
# 初始化用户池
curl -X POST "http://localhost:8080/api/init?size=10000"

# 预期：生成10000个用户，标签分布符合配置比例
```

### 第三步：验证流量分配
```bash
# 启动压测
curl -X POST "http://localhost:8080/api/start-comparison?qps=100&duration=10"

# 预期：每个客群的比例接近目标（10%, 20%, 30%, 40%）
```

### 第四步：验证MSE计算
```bash
# 查看指标
curl http://localhost:8080/api/metrics

# 预期：MSE越小越好，标签分布符合原始比例
```

## 🚀 下一步

1. **创建应用服务**：ExperimentApplicationService
2. **创建配置类**：ExperimentConfig
3. **创建控制器**：ExperimentController
4. **创建指标服务**：MetricsService
5. **测试验证**：逐步验证每个功能

## 📝 关键代码示例

### 创建实验
```java
Experiment experiment = experimentFactory.createDefaultExperiment();
// 4个分组：A(10%), B(20%), C(30%), D(40%)
// 3个标签：性别(2), 省份(3), 年龄(3)
// 18种组合
```

### 分配流量
```java
User user = new User("user_001", new int[]{0, 1, 2}, tagSchema);
AssignmentResult result = assignmentService.assign(user, experiment);
// result.getGroupId() = "A" | "B" | "C" | "D"
```

### 计算MSE
```java
// MSE = Σ(实际比例 - 目标比例)²
// 对于4个客群：
// MSE = (actualA - 0.10)² + (actualB - 0.20)² + (actualC - 0.30)² + (actualD - 0.40)²
```

---

**状态**: 🚧 开发中
**预计完成时间**: 2026-03-03 20:00
