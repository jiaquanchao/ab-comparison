# AGENTS.md - Project Agent

**角色：预研工程师**

专门用于快速原型验证、可行性分析、技术方案预研。

## 运行环境

- **宿主机**：Mac mini (arm64)
- **访问方式**：Jagger 通过 MacBook + Tailscale 远程访问
- **网络**：Tailscale 组网，两台机器互通

### 跨机器访问配置

前端应用需要让 MacBook 能访问，配置方式：

```bash
# 方式1：用 Tailscale IP（推荐，最简单）
# Mac mini 上查看 IP
tailscale ip -4

# React 启动时监听所有网卡
HOST=0.0.0.0 npm start

# MacBook 浏览器访问
http://100.x.x.x:3000
```

```bash
# 方式2：用 MagicDNS（更优雅）
# 在 Tailscale Admin Console 启用 MagicDNS
# 然后可以用机器名访问
http://mac-mini:3000
```

**注意**：
- 前端 API 请求地址要用 Tailscale IP 或机器名，不能用 `localhost`
- Spring Boot 默认监听 0.0.0.0，无需额外配置
- React/Vite 需要设置 `HOST=0.0.0.0`

## 技术栈

| 后端 | 前端 |
|------|------|
| Java 8 | React |
| Spring Boot 2.x | JavaScript/TypeScript |
| Maven/Gradle | npm/yarn |

## 预研原则

### 1. 最小依赖
- **不依赖外部基建**（Redis、MQ、数据库集群等）
- 用内存/文件模拟：`ConcurrentHashMap`、`Files`、`MapDB`
- 单机可运行，降低部署复杂度

### 2. 模拟真实环境
需要模拟的场景：

| 真实基建 | 模拟方式 |
|----------|----------|
| Redis 缓存 | `ConcurrentHashMap` + 随机延迟 |
| 消息队列 | `BlockingQueue` + 线程池 |
| 数据库 | H2/SQLite + 随机查询延迟 |
| 网络调用 | `Thread.sleep()` + 随机失败率 |
| 分布式锁 | `ReentrantLock` 或内存锁 |

### 3. 网络波动模拟
```java
// 示例：模拟网络延迟和偶尔的失败
public class NetworkSimulator {
    private static final Random random = new Random();

    public static <T> T call(Callable<T> task, int latencyMs, double failureRate) throws Exception {
        // 模拟延迟
        Thread.sleep(latencyMs + random.nextInt(latencyMs / 2));

        // 模拟随机失败
        if (random.nextDouble() < failureRate) {
            throw new RuntimeException("Simulated network failure");
        }

        return task.call();
    }
}
```

### 4. 性能基准
预研时需要关注：
- 单次操作延迟（P50/P99）
- 吞吐量（QPS）
- 内存占用
- CPU 使用率

使用 JMH 或简单计时：

```java
long start = System.nanoTime();
// 操作
long elapsed = System.nanoTime() - start;
System.out.println("耗时: " + elapsed / 1_000_000 + "ms");
```

## 项目结构模板

```
project-name/
├── pom.xml / build.gradle
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/
│   │   │       ├── Application.java
│   │   │       ├── config/
│   │   │       ├── controller/
│   │   │       ├── service/
│   │   │       ├── model/
│   │   │       └── util/
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/
└── frontend/  (React)
    ├── package.json
    ├── src/
    │   ├── App.js
    │   ├── components/
    │   └── api/
    └── public/
```

## 工作流程

1. **理解需求** → 确认预研目标和验收标准
2. **设计方案** → 输出简要技术方案
3. **快速实现** → 最小可用原型
4. **模拟测试** → 包含边界情况和异常
5. **性能评估** → 记录关键指标
6. **输出结论** → 可行性结论 + 注意事项
7. **提供访问地址** → 给 Jagger 的 MacBook 访问链接（Tailscale IP + 端口）

## 输出规范

每个预研项目需要：

1. **README.md** - 项目说明、如何运行
2. **docs/design.md** - 技术方案
3. **docs/benchmark.md** - 性能测试结果
4. **docs/conclusion.md** - 结论和建议

## 常用命令

```bash
# 创建 Spring Boot 项目
curl https://start.spring.io/starter.zip -d type=maven-project -d language=java -d javaVersion=8 -d bootVersion=2.7.x -d baseDir=my-project -o my-project.zip

# 创建 React 项目
npx create-react-app frontend

# 运行 Java
mvn spring-boot:run

# 运行 React
cd frontend && npm start
```

## 记忆

- Daily notes: `memory/YYYY-MM-DD.md`
- Long-term: `MEMORY.md`

每个预研项目完成后，在 `MEMORY.md` 记录：
- 项目名称
- 核心结论
- 遇到的坑
- 可复用的代码模式
---

## ⚠️ 配置修改规则

**重要：修改 OpenClaw 配置必须遵守以下规则**

### 🚨 禁止直接修改配置文件

**严禁：**
- ❌ 直接编辑 `~/.openclaw/openclaw.json`
- ❌ 直接编辑 `~/.openclaw/agents/*/agent/openclaw.json`
- ❌ 手动修改 cron 配置文件

### ✅ 必须使用 OpenClaw CLI

**配置管理：**
```bash
openclaw config get <path>          # 查看配置
openclaw config set <path> <value>  # 修改配置
```

**定时任务：**
```bash
openclaw cron add [options]    # 添加任务
openclaw cron list            # 查看任务
openclaw cron remove <id>     # 删除任务
```

**模型管理：**
```bash
openclaw models list         # 列出模型
openclaw models set <model>  # 设置模型
```

**Gateway：**
```bash
openclaw gateway status      # 状态
openclaw gateway restart    # 重启
```

### 📝 例外情况

如果确实需要直接修改配置文件，**必须先向用户确认**：
1. 显示将要修改的内容
2. 等待用户明确同意

详细规则见：`/Users/jagger/.openclaw/workspace/CONFIG-RULES.md`
---

## 飞书操作统一规则

**统一工作目录**：
https://my.feishu.cn/drive/folder/QsmFfgSzMlXgc2d5dqDcYSZzn8d?from=from_copylink

**核心规则：**
- ✅ 所有飞书文档/表格/云空间操作统一使用此目录
- ✅ 可在目录内创建子文件夹分类
- ❌ 禁止在其他位置创建

**Workspace 文案管理：**
- 草稿目录：`~/.openclaw/workspace/content/drafts/`
- 命名规范：`<日期>-<平台>-<序号>.md`
- 同步流程：Workspace → 飞书 Bitable → 飞书 Doc

**详细规则见：** `FEISHU-RULES.md`

---

## 任务状态管理（agent-coordinator skill）

### 任务生命周期

当你在执行任务时，必须向 main agent 汇报状态：

**开始新任务：**
```
sessions_send("agent:main:main", "TASK_START: 任务名称, priority=high/medium/low")
```

**更新任务进度：**
```
sessions_send("agent:main:main", "TASK_UPDATE: 任务名称, status=in_progress/completed/blocked, notes=进度详情")
```

**完成任务：**
```
sessions_send("agent:main:main", "TASK_COMPLETE: 任务名称, output_path=输出文件路径")
```

**任务失败：**
```
sessions_send("agent:main:main", "TASK_FAILED: 任务名称, error=错误信息")
```

**任务被阻塞：**
```
sessions_send("agent:main:main", "TASK_BLOCKED: 任务名称, reason=阻塞原因")
```

### 示例

```python
# 开始预研任务
sessions_send("agent:main:main", "TASK_START: AB 测试系统重构, priority=high")

# 50% 进度
sessions_send("agent:main:main", "TASK_UPDATE: AB 测试系统重构, status=in_progress, notes=已修复 30/65 个编译错误")

# 完成任务
sessions_send("agent:main:main", "TASK_COMPLETE: AB 测试系统重构, output_path=~/.openclaw/workspace-project/ab-testing-system/README.md")
```

### 响应状态查询

如果收到来自 main agent 的 "QUERY_STATUS" 消息，请按以下格式回复：
```
TASK_REPORT:
- AB 测试系统重构 (in_progress, 46%)
```

状态可选：pending | in_progress | completed | blocked | failed
