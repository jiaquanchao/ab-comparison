# TOOLS.md - Local Notes

Skills define _how_ tools work. This file is for _your_ specifics — the stuff that's unique to your setup.

## 网络环境

- **宿主机**：Mac mini (arm64)
- **访问者**：Jagger 的 MacBook，通过 Tailscale 连接
- **Mac mini Tailscale IP**：`100.73.204.30`
- **Mac mini LAN IP**：`192.168.31.235`

### 验收地址格式
```
http://100.73.204.30:<port>  # Tailscale
http://192.168.31.235:<port> # LAN
```

## Claude Code 配置

- **安装路径**：`/Users/jagger/.nvm/versions/node/v22.19.0/bin/claude`
- **版本**：`1.0.109 (Claude Code)`
- **Node 版本**：`v24.7.0` (系统当前版本)
- **启动命令**：`/Users/jagger/.nvm/versions/node/v22.19.0/bin/claude "你的任务"`
- **必须参数**：`pty: true`（交互式终端需要）
- **推荐参数**：`background: true`（长时间任务）

### 使用示例
```bash
# 启动 Claude Code
/Users/jagger/.nvm/versions/node/v22.19.0/bin/claude "任务描述"

# 通过 OpenClaw exec 启动（推荐）
exec pty:true background:true workdir:/path/to/project command:"/Users/jagger/.nvm/versions/node/v22.19.0/bin/claude '任务描述'"
```

### 注意事项
- 需要在 git 项目目录下运行（或先 `git init`）
- 首次运行会询问是否信任目录
- 使用 `process action:submit data:"2"` 允许所有编辑
- 使用 `process action:poll` 监控进度

## Why Separate?

Skills are shared. Your setup is yours. Keeping them apart means you can update skills without losing your notes, and share skills without leaking your infrastructure.
