# 测试用例

自动化：`mvn test`  
手工：服务起来后打开 `http://localhost:8080`，按 TC-M 执行。

## 1. 自动化（已实现）

| 编号 | 类 | 断言 |
|------|----|------|
| TC-A01 | `TicketServiceTest#createAssignAndClose` | 创建为待受理；分派变处理中；关单变已关闭且详情含原因 |
| TC-A02 | `TicketServiceTest#queryByUser` | 按 userId 过滤，只返回该用户工单 |
| TC-A03 | `MockLlmClientTest#repairCreatesTicketTool` | 「卫生间漏水，帮忙报修」产出 `create_ticket`，参数含报事 |
| TC-A04 | `MockLlmClientTest#afterToolReturnsText` | 已有 tool 消息时只回文本、不再调工具 |
| TC-A05 | `ChatServiceTest#handoffSkipsAi` | 「转人工」后 mode=HUMAN，有助手提示句 |
| TC-A06 | `ChatServiceTest#takeoverAndAgentReply` | 接管后坐席回复成为最后一条 |
| TC-A07 | `WeComCallbackControllerTest#verifyEcho` | GET echostr=ping-1 回 ping-1 |
| TC-A08 | `WeComCallbackControllerTest#inboundCreatesSession` | POST 门禁报修返回 ok 且 userId=wx-demo |

跑完期望：Tests run: 8, Failures: 0。

## 2. 手工验收

### TC-M01 冷启动

步骤：不设 `DASHSCOPE_API_KEY`，启动，打开首页。  
期望：页顶为「模型：Mock（未配置 DASHSCOPE_API_KEY）」；左右栏可见。

### TC-M02 报修建单

步骤：左侧点「报修」。  
期望：约 1 秒内助手回复已记录；底部工单表多一行，类型报事、状态待受理、单号 `Txxxx`。

### TC-M03 投诉建单

步骤：点「投诉」。  
期望：新建类型=投诉、等级=高的工单。

### TC-M04 查工单

步骤：点「查工单」。  
期望：助手回复里能看到刚才的单号或「当前没有工单。」（若先点这个）。

### TC-M05 转人工

步骤：点「转人工」，坐席点「接管」，输入「我是坐席，马上帮您看」发送。  
期望：模式变为「人工接管中」；左右都能看到坐席这句话；此后业主再发「还在吗」不再出现 AI 长回复。

### TC-M06 交回 AI

步骤：点「交回 AI」，业主再发「电梯响」。  
期望：模式回到 AI；助手再次回复；可能再出工单。

### TC-M07 双用户隔离

步骤：切到刘女士发「门禁坏了」，再切回陈先生。  
期望：两边历史不串；工单 `userId` 不同。

### TC-M08 回调验活

```bash
curl -s "http://localhost:8080/wecom/callback?echostr=hello"
curl -s -H "Content-Type: application/json" -d "{\"FromUserName\":\"wx-curl\",\"Content\":\"报修水管\"}" http://localhost:8080/wecom/callback
```

期望：第一下 `hello`；第二下 `{"ok":true,...}`；工作台出现 wx-curl。

### TC-M09 缺参

`POST /api/customer/send` body `{}`。  
期望：HTTP 400，`error` 含「缺少 userId」。

### TC-M10 真实千问（可选）

设好 `DASHSCOPE_API_KEY` 重启。  
期望：页顶显示真实模型名；报修仍能建单。失败时应出现「模型暂时不可用…转人工」，进程不挂。

## 3. 不测 / 已知边界

- 官方 AES 包体解密
- 多实例会话同步
- 千问账单与限流
- SSE 断线重连后的补历史（刷新页面即可）
- 工单号并发到千万级（演示用 AtomicInteger）

## 4. 回归建议

改 `ChatService`、`TicketService`、回调控制器后，至少跑 `mvn test` + TC-M02 + TC-M05。
