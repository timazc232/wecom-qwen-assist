# 设计说明

对应实现：`src/main/java/com/zhangchong/assist`  
需求见 [REQUIREMENTS.md](REQUIREMENTS.md)

## 1. 一句话

所有入口汇到 `ChatService`。先判转人工，再按会话排队调模型；模型通过工具碰工单，界面靠 SSE 刷新。

## 2. 模块

```
业主微信 / 企微回调
        │
        ▼
  CustomerController / WeComCallbackController
        │
        ▼
     ChatService ──────► LlmClient (Qwen / Mock)
        │                      │
        │                      ▼
        │                 TicketService
        │
        ├── SessionStore（按 userId）
        └── SseHub ──► 坐席工作台 / 业主面板
```

| 包 | 职责 |
|----|------|
| `wecom` | URL 验证、明文入站 |
| `chat` | 会话、排队、转人工、组提示词 |
| `llm` | 千问 HTTP + Mock |
| `ticket` | 工单 CRUD |
| `api` | 页面 API 与 SSE |
| `config` | `assist.qwen` / `assist.wecom` |

## 3. 关键设计

### 3.1 同一会话排队

`ChatSession` 内有队列和 `busy` 标志。同一用户的 AI 任务串行，不同用户走缓存线程池。

原因：微信回调可能连发，并行走模型会乱序，简历里的「同会话顺序」必须能当面点出来。

不做全局单线程，避免所有用户互相堵住。

### 3.2 人工优先于模型

`onUserMessage` 顺序：

1. 落库用户消息并广播
2. 命中转人工关键词 → 切 `HUMAN`，回系统句，return
3. 已是 `HUMAN` → 只广播，不调模型
4. 否则入队 `replyByAi`

坐席 `takeover` 也会切模式并写一条系统消息。`release` 交回后，下一条业主消息才再走 AI。

### 3.3 工具调用循环

最多 3 轮：

```
messages(system + 近 12 条)
    → LlmClient.chat
    → 无 tool_calls：把文本当助手回复
    → 有 tool_calls：执行工具，把 tool 结果追加后再调
```

工具失败以字符串返回给模型，不把异常抛到业主侧。超过 3 轮停止并提示转人工。

### 3.4 有 key 走千问，没 key 走 Mock

`AssistProperties.Qwen.hasKey()` 决定客户端。Mock 按关键词触发建单/查询，保证无网也能演示。

千问走 DashScope OpenAI 兼容接口：`{base-url}/chat/completions`。默认模型 `qwen-plus`，可用 `QWEN_MODEL` 改成控制台里真实有的名字（如 `qwen3.6`）。

### 3.5 存储

`ConcurrentHashMap` 存会话和工单。进程退出即空。演示足够；上生产要换 MySQL，会话排队可换成 Redis 队列/租约。

### 3.6 界面

单页静态资源，无前端构建。左右双栏：业主像微信，坐席像工作台。SSE 事件名：`session`、`ticket`。

## 4. 数据

**消息：** `id, role, content, channel, time`  
role = `user` | `assistant` | `agent` | `system`

**会话：** `userId, userName, mode, messages, unread, queue`

**工单：** `id, userId, type, title, detail, level, status, assignee, createdAt, updatedAt`

状态：待受理 → 处理中 → 已关闭。

## 5. 接口一览

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/wecom/callback` | echostr 原样返回 |
| POST | `/wecom/callback` | 企微/微信入站 |
| POST | `/api/customer/send` | 页面模拟业主发送 |
| GET | `/api/customer/{userId}` | 业主侧会话 |
| GET | `/api/meta` | 当前模型名、是否真模型 |
| GET | `/api/sessions` | 会话摘要列表 |
| GET | `/api/sessions/{userId}` | 会话详情（清未读） |
| POST | `/api/sessions/{userId}/takeover` | 接管 |
| POST | `/api/sessions/{userId}/release` | 交回 |
| POST | `/api/sessions/{userId}/reply` | 坐席回复 |
| GET | `/api/tickets` | 全部工单 |
| GET | `/api/events` | SSE |

非法参数返回 HTTP 400，`{"error":"..."}`。

## 6. 取舍

| 选择 | 放弃 | 原因 |
|------|------|------|
| 明文回调 | 官方加解密 | 本地 1 分钟能跑，面试能讲清和生产的差别 |
| 内存 | DB | 降低启动成本 |
| Java 8 + Boot 2.7 | Boot 3 / Java 21 | 本机和多数面试官环境都能编 |
| Mock 兜底 | 强制云调用 | 克隆即用 |
| 关键词转人工 | 只靠模型判断 | 转人工必须稳，不能等模型抽风 |

## 7. 和简历的对应

面试只讲四段：回调进会话、同用户排队、工具建单、转人工停模型。生产可可多出来的路由、记忆、租约、脱敏，明确说「这个仓库没做」。
