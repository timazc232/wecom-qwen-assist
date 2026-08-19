# 物业助手 Demo

微信 / 企微回调 + 通义千问辅助回复 + 工单工具调用 + 人工接管。

这是一个能本地打开的最小生产链路，用来说明「AI 辅助人工」怎么接到真实客服流程里，不是调一次模型 API。

对应简历项目：**智能客服可可** 的缩小版。

## 文档

| 文档 | 内容 |
|------|------|
| [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) | 需求：角色、功能、非功能、验收 |
| [docs/DESIGN.md](docs/DESIGN.md) | 设计：模块、排队、工具循环、取舍 |
| [docs/TESTCASES.md](docs/TESTCASES.md) | 测试用例：8 条自动化 + 10 条手工 |
| [docs/DEPLOY.md](docs/DEPLOY.md) | 上线手册：启动、检查、回滚、排障 |

## 它演示什么

| 能力 | 怎么体现 |
|------|----------|
| 通道接入 | `GET/POST /wecom/callback`，兼容微信 URL 验证和明文入站 |
| 同会话排队 | 同一用户的 AI 回复串行处理，后到的消息不会插队 |
| 千问辅助 | 配置 `DASHSCOPE_API_KEY` 走通义千问；不配 key 用内置 Mock，页面仍可完整体验 |
| 工单工具调用 | `create_ticket` / `query_tickets` / `assign_ticket` / `close_ticket` |
| 人工接管 | 用户说「转人工」，或坐席点接管；之后只走人工回复 |
| 人机交回 | 坐席可把会话交回 AI |

口径和简历一致：**模型辅助人工，人不退出主责。**

## 30 秒打开

需要 JDK 8+、Maven 3.8+。

```bash
git clone https://github.com/timazc232/wecom-qwen-assist.git
cd wecom-qwen-assist
mvn spring-boot:run
```

浏览器打开 [http://localhost:8080](http://localhost:8080)

左侧是业主微信，右侧是坐席工作台。

### 建议点这 4 句

1. 「卫生间漏水，帮忙报修」→ 创建报事工单  
2. 「物业费收错了，我要投诉」→ 创建投诉工单  
3. 「查一下我的工单进度」→ 查询工具  
4. 「转人工」→ 坐席接管，再在右侧人工回复  

## 接真实千问

```bash
# Windows PowerShell
$env:DASHSCOPE_API_KEY="sk-xxx"
$env:QWEN_MODEL="qwen-plus"
mvn spring-boot:run
```

默认走阿里云 DashScope 的 OpenAI 兼容接口。模型名按控制台实际可用的改，例如 `qwen-plus` / `qwen-turbo` / `qwen3.6`。

不配 key 时，页面顶部会显示 `模型：Mock`，报修/投诉/转人工同样能走完。

## 回调怎么接

本地演示用明文 JSON，避免一上来就搭企业微信解密。生产里可可走的是官方签名 + AES，这一层刻意没假装已经接好。

URL 验证：

```http
GET /wecom/callback?echostr=hello
```

入站：

```http
POST /wecom/callback
Content-Type: application/json

{"FromUserName":"wx-001","Content":"电梯困人"}
```

页面发送走的是 `/api/customer/send`，内部和回调进同一个 `ChatService`。

## 结构

```
src/main/java/com/zhangchong/assist
  wecom/     回调入口
  chat/      会话、同会话排队、转人工
  llm/       千问客户端 + Mock
  ticket/    工单工具
  api/       坐席、业主、SSE
```

内存存储，关掉进程数据清空。够演示，不够当生产。

## 面试时可以怎么讲

> 这是可可的最小切片：回调进会话，同一用户排队，千问决定是否调工单工具，用户要转人工就停模型、交给坐席。生产上还会补租约恢复、项目路由、长期记忆和脱敏；这个仓库只把能当面点开的主链路留下。

## License

MIT
