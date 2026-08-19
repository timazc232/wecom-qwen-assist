# 上线手册

适用：本机演示、内网一台机给面试官点、或自己的云主机。  
这不是万科生产发布流程。内存存储，重启数据丢，不要当正式客服用。

## 1. 环境

| 项 | 要求 |
|----|------|
| JDK | 8 及以上，`java -version` 能出字 |
| Maven | 3.8+，或用本机已装的 `mvn` |
| 端口 | 默认 8080，没被占 |
| 可选 | `DASHSCOPE_API_KEY`，阿里云百炼/DashScope |

```bash
java -version
mvn -version
```

## 2. 获取代码

```bash
git clone https://github.com/timazc232/wecom-qwen-assist.git
cd wecom-qwen-assist
```

## 3. 配置

不配也能起，走 Mock。

### 3.1 环境变量（推荐）

PowerShell：

```powershell
$env:DASHSCOPE_API_KEY="sk-你的key"
$env:QWEN_MODEL="qwen-plus"
$env:WECOM_TOKEN="demo-token"
```

cmd：

```bat
set DASHSCOPE_API_KEY=sk-你的key
set QWEN_MODEL=qwen-plus
```

Linux / macOS：

```bash
export DASHSCOPE_API_KEY=sk-你的key
export QWEN_MODEL=qwen-plus
```

模型名改成控制台里真实有的，例如 `qwen-plus`、`qwen-turbo`、`qwen3.6`。

### 3.2 不要做的事

- 不要把 key 写进仓库里的 `application.yml` 再提交。
- 需要本地文件时，用未跟踪的 `application-local.yml`，或只在启动命令里传。

可选本地文件（已 gitignore）：

```yaml
assist:
  qwen:
    api-key: sk-xxx
    model: qwen-plus
```

启动：`mvn spring-boot:run -Dspring-boot.run.profiles=local`

## 4. 启动

开发：

```bash
mvn spring-boot:run
```

打包：

```bash
mvn -DskipTests package
java -jar target/wecom-qwen-assist-1.0.0.jar
```

换端口：

```bash
java -jar target/wecom-qwen-assist-1.0.0.jar --server.port=8081
```

日志里出现 `Tomcat started on port(s): 8080` 即成功。

## 5. 发布检查

按顺序勾：

1. `mvn test` 全绿  
2. 打开 http://localhost:8080 ，页顶模型状态符合预期  
3. 点「报修」出工单  
4. 点「转人工」，坐席能回复  
5. `curl http://localhost:8080/wecom/callback?echostr=ok` 回 `ok`  
6. 仓库里没有 `sk-` 一类密钥  

更细步骤见 [TESTCASES.md](TESTCASES.md) 的 TC-M01～M09。

## 6. 给别人看时

本机：

- 关公司 VPN 对 localhost 的干扰（偶发）。
- 浏览器建议 Chrome。
- 先走 Mock，避免演示时卡在欠费或限流。

云主机：

- 安全组放行 8080，或前面加 Nginx 反代到 80/443。
- 公网不要裸奔无鉴权的坐席接口。要给外人点，用 SSH 隧道：

```bash
ssh -L 8080:127.0.0.1:8080 user@your-host
```

本地再打开 http://localhost:8080。

接真微信/企微：

1. 准备公网 HTTPS（官方要求）。
2. 回调填 `https://你的域名/wecom/callback`。
3. Token 与 `WECOM_TOKEN` 一致。
4. 当前实现**不能**解官方 AES 包。要接生产包体，得按企业微信文档补验签和解密，再把明文丢进 `ChatService.onUserMessage`。在面试里主动说这层还没做。

## 7. 常见问题

| 现象 | 处理 |
|------|------|
| 端口占用 | `--server.port=8081` 或杀掉占用 8080 的进程 |
| `mvn` 不是命令 | 安装 Maven 3.8+，并加入 PATH |
| 编译失败，Java 版本 | 确认是 JDK 不是仅 JRE；主版本 ≥ 8 |
| 页顶 Mock，但你配了 key | 变量要在**启动前**设置，改完必须重启 |
| 千问 401/403 | key 错或模型名不在账户权限里，先改回 Mock |
| 点了报修没工单 | 等 1～2 秒；看后台是否有 `Qwen call failed` |
| SSE 不刷新 | 硬刷新页面；或手动点会话 |

## 8. 停机与回滚

停：终端 Ctrl+C，或结束 Java 进程。没有外部库要迁。

回滚：重新 clone 上一个 tag/commit，再 `mvn spring-boot:run`。没有数据库脚本。

## 9. 版本

当前制品：`wecom-qwen-assist-1.0.0.jar`  
默认端口：8080  
健康观察：进程在、首页 200、`GET /api/meta` 返回 JSON。
