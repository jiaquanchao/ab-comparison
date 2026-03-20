# MEMORY.md - Proto 长期记忆

最后更新: 2026-03-04

## 环境配置

### 宿主机
- **设备**: Mac mini (arm64)
- **系统**: macOS 26.3
- **Node**: v24.7.0 (已从 v22.19.0 升级)

### 网络访问
- **局域网 IP**: `192.168.31.235`
- **Tailscale IP**: `100.73.204.30`
- **公网 IP**: `139.226.2.242`
- **Tailscale**: v1.90.1 运行中

### 访问格式
```
http://192.168.31.235:<port>  # 局域网
http://100.73.204.30:<port>   # Tailscale (推荐)
```

## 开发工具

### Claude Code
- **路径**: `/Users/jagger/.nvm/versions/node/v22.19.0/bin/claude`
- **启动**: `exec pty:true background:true workdir:<project> command:"/Users/jagger/.nvm/versions/node/v22.19.0/bin/claude '任务'"`
- **必须参数**: `pty: true`
- **推荐参数**: `background: true`
- **用途**: 所有代码开发任务必须用 Claude Code

### 网络工具
- 网络命令不在 PATH 中，需使用完整路径
- `/sbin/ifconfig` 查看网络配置
- `/Applications/Tailscale.app/Contents/MacOS/Tailscale` Tailscale CLI

## 预研原则

1. **最小依赖**: 不依赖外部基建（Redis、MQ、数据库集群）
2. **模拟真实**: 用内存/文件模拟基建
3. **跨机访问**: 前端监听 `0.0.0.0`，用 Tailscale IP 访问
4. **快速验证**: 先跑起来再优化

## 飞书操作规则

### 🚨 必须遵守的核心规则
- **统一工作目录**：https://my.feishu.cn/drive/folder/QsmFfgSzMlXgc2d5dqDcYSZzn8d
- ✅ 所有飞书文档/表格/云空间操作必须在此目录下
- ✅ 可在目录内创建子文件夹分类
- ❌ 严格禁止在其他位置创建飞书资源

### 操作前检查
- [ ] 文档/表格/文件夹在 my_oc 目录下
- [ ] 链接包含 my.feishu.cn/drive/folder/QsmFfgSzMlXgc2d5dqDcYSZzn8d
- [ ] 不在用户个人空间或团队空间的其他位置

### 当前状态
- **飞书资源维护**: 无（作为预研工程师，主要做本地技术预研）
- **最后检查**: 2026-03-19

## 已完成项目

### ab-testing-system - A/B 实验动态分流系统
- **状态**: ✅ 预研完成
- **位置**: `ab-testing-system/`
- **时间**: 2026-03-01
- **详情**: `memory/2026-03-01.md`

**核心挑战**:
- 1000万+ 用户规模
- 5维标签 × 10值 = 10^5 组合
- QPS 10,000+，延迟 < 50ms
- 实时分布对齐 + 粘性分流

**技术选型**:
- Java 17 (Record, Var, Virtual Thread)
- Spring Boot 3.x
- Caffeine (本地缓存)
- React + TailwindCSS + ECharts

## 当前状态

- **Workspace**: `/Users/jagger/.openclaw/workspace-project`
- **Gateway**: 正常运行，本地模式
- **Channels**: Telegram ✅ Feishu ✅
- **Memory 索引**: 未启用（0 files · 0 chunks）

## 待办

- [ ] 更新 TOOLS.md 中的 Node 版本
- [ ] 启用 OpenClaw memory 索引
