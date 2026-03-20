# A/B 实验动态分流系统

高性能 A/B 实验分流压测引擎，对比两种分流方案的性能和准确性。

## 核心特性

- ✅ 1000万+ 用户规模
- ✅ QPS 10,000+，延迟 < 50ms
- ✅ 实时分布对齐（各组标签分布 = 大盘分布）
- ✅ 粘性分流（同一用户结果不变）
- ✅ WebSocket 实时推送指标

## 方案对比

| 维度 | 方案一：贪心计数 | 方案二：自适应哈希 |
|------|-----------------|-------------------|
| 一致性 | 强一致 | 最终一致 |
| 性能 | 锁竞争严重 | 热路径无锁 |
| 复杂度 | 低 | 中 |
| 适用场景 | 低 QPS | 高 QPS |

## 快速开始

### 后端

```bash
# 编译
mvn clean package

# 运行
java -jar target/ab-testing-system-1.0.0-SNAPSHOT.jar
```

后端运行在：http://localhost:8080

### 前端

```bash
cd frontend
npm install
npm start
```

前端运行在：http://localhost:3000

## API 接口

| 接口 | 说明 |
|------|------|
| POST /api/init?size=10000000 | 初始化用户池 |
| POST /api/start?qps=1000&duration=60 | 启动压测 |
| POST /api/stop | 停止压测 |
| POST /api/reset | 重置状态 |
| GET /api/status | 获取当前状态 |

## 技术栈

### 后端
- Java 17
- Spring Boot 3.2
- HdrHistogram（延迟统计）
- Caffeine（缓存）

### 前端
- React 18
- TailwindCSS
- ECharts
- WebSocket (STOMP)

## 目录结构

```
ab-testing-system/
├── pom.xml
├── src/main/java/com/example/abtesting/
│   ├── model/           # 数据模型
│   ├── service/         # 分流引擎
│   ├── controller/      # REST API
│   ├── config/          # 配置
│   └── util/            # 工具类
├── src/main/resources/
│   └── application.yml
└── frontend/
    ├── src/
    │   ├── components/  # React 组件
    │   └── App.js
    └── package.json
```

## 验收地址

前端：http://<tailscale-ip>:3000

## 文档

- [设计文档](docs/design.md)
- [性能测试](docs/benchmark.md)
- [结论和建议](docs/conclusion.md)
