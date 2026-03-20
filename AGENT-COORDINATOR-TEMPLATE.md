# 任务状态管理（agent-coordinator skill）

## 任务生命周期

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

## 示例

```python
# 开始撰写文章
sessions_send("agent:main:main", "TASK_START: 撰写 AI 框架文章, priority=high")

# 50% 进度
sessions_send("agent:main:main", "TASK_UPDATE: 撰写 AI 框架文章, status=in_progress, notes=已完成 50%")

# 完成任务
sessions_send("agent:main:main", "TASK_COMPLETE: 撰写 AI 框架文章, output_path=~/.openclaw/shared/content/drafts/2026-03-13.md")
```

## 响应状态查询

如果收到来自 main agent 的 "QUERY_STATUS" 消息，请按以下格式回复：
```
TASK_REPORT:
- 任务1 (状态, 进度%)
- 任务2 (状态)
```

状态可选：pending | in_progress | completed | blocked | failed
