# 设计文档

## 1. 系统架构

```
┌─────────────────────────────────────────────┐
│             LoadTestController               │
│   (QPS控制、流量偏置、用户池管理)              │
└─────────────────┬───────────────────────────┘
                  │
        ┌─────────┴──────────┐
        │                    │
   ┌────▼─────┐        ┌────▼──────┐
   │ Greedy   │        │ Adaptive  │
   │ Engine   │        │ Hash      │
   └────┬─────┘        └────┬──────┘
        │                    │
        └─────────┬──────────┘
                  │
         ┌────────▼────────┐
         │ MetricCollector  │
         │ (HdrHistogram)   │
         └────────┬─────────┘
                  │
         ┌────────▼────────┐
         │ WebSocket Push   │
         │ (100ms 推送)     │
         └─────────────────┘
```

## 2. 方案一：强一致性贪心平衡

### 核心思路
- 实时查询标签组合的当前分配情况
- 优先分发给占比低于目标的组
- 使用分段锁模拟 Redis 锁竞争

### 代码关键点

```java
// 分段锁（故意缩小数量模拟竞争）
Lock lock = locks[Math.abs(comboKey.hashCode()) % LOCK_STRIDE];
lock.lock();
try {
    // 模拟 Redis 往返延迟
    simulateIO(REDIS_DELAY_NS);

    // 贪心决策
    double currentARatio = (double) countA / total;
    group = currentARatio < TARGET_A_RATIO ? "A" : "B";

    // 更新计数器
    counts[group.equals("A") ? 0 : 1].increment();
} finally {
    lock.unlock();
}
```

### 性能瓶颈
- 锁竞争严重（1024个锁，10^5个组合）
- 延迟高（20ms HBase + 2ms Redis × 锁等待）

## 3. 方案二：自适应哈希反馈

### 核心思路
- 基础分流依赖确定性哈希（MurmurHash3）
- 异步反馈环每100ms调整阈值
- 热路径无锁，性能极高

### 代码关键点

```java
// 热路径：无锁读取阈值 + 哈希计算
double threshold = thresholdMap.getOrDefault(comboKey, INITIAL_THRESHOLD);
int hash = hashUserId(user.userId());
String group = (Math.abs(hash) % 100) < threshold ? "A" : "B";

// 后台线程：每100ms调整阈值
public void adjustThresholds() {
    double currentARatio = (double) countA / total;
    double error = TARGET_A_RATIO - currentARatio;
    double adjustment = error * 10; // 调整系数
    double newThreshold = currentThreshold + adjustment;
    thresholdMap.put(comboKey, newThreshold);
}
```

### 性能优势
- 热路径无锁
- 延迟低（仅20ms HBase模拟）

## 4. 指标收集

### 延迟统计
- 使用 HdrHistogram 记录完整延迟分布
- 支持 P50/P99/P999 分位数查询
- 每100ms重置一次

### 分布对齐
- 计算各组与大盘的标签分布 MSE
- MSE = (1/n) * Σ(P_group - P_total)²
- 目标：MSE < 0.001

## 5. 压测控制

### QPS 平滑发放
```java
long intervalNanos = 1_000_000_000L / targetQps;
long nextRequestTime = System.nanoTime() + intervalNanos;
LockSupport.parkNanos(nextRequestTime - System.nanoTime());
```

### 流量偏置
- 前10秒只推特定标签的用户
- 观察方案二的反馈调节需要多久拉回分布

## 6. 前端设计

### 实时看板
- 每100ms通过 WebSocket 推送指标
- 使用 ECharts 渲染图表
- TailwindCSS 样式

### 图表类型
1. **分布对比图**：分组柱状图，展示各组标签分布
2. **延迟直方图**：热力图，展示延迟分布
3. **MSE 趋势**：折线图，展示分布偏差趋势
