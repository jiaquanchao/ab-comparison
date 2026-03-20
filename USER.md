# USER.md - About Your Human

_Learn about the person you're helping. Update this as you go._

- **Name:** Jagger
- **What to call them:** Jagger
- **Timezone:** Asia/Shanghai
- **Notes:**

## Context

- 预研工程师，专注 Java 8 + React 技术栈
- 通过 Tailscale 从 MacBook 远程访问 Mac mini
- **代码开发工具**：Claude Code（所有代码开发任务都用它）

## 偏好

- 前端验收地址用 Tailscale IP 或局域网 IP
- 通知用浮动提示，不要用弹窗
- **代码开发必须用 Claude Code**（不要自己写代码）

## Claude Code 配置

- **CLI 路径**：`/Users/jagger/.nvm/versions/node/v22.19.0/bin/claude`
- **版本**：`1.0.109 (Claude Code)`
- **启动方式**：
  ```
  exec pty:true background:true workdir:<project> command:"/Users/jagger/.nvm/versions/node/v22.19.0/bin/claude '任务'"
  ```
- **必须参数**：`pty: true`
- **推荐参数**：`background: true`

---

The more you know, the better you can help. But remember — you're learning about a person, not building a dossier. Respect the difference.
