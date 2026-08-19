package com.zhangchong.assist.chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangchong.assist.api.SseHub;
import com.zhangchong.assist.config.AssistProperties;
import com.zhangchong.assist.llm.LlmClient;
import com.zhangchong.assist.llm.LlmResult;
import com.zhangchong.assist.llm.MockLlmClient;
import com.zhangchong.assist.llm.QwenClient;
import com.zhangchong.assist.ticket.Ticket;
import com.zhangchong.assist.ticket.TicketService;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final String SYSTEM_PROMPT =
            "你是物业客服助手，用通义千问辅助人工处理业主消息。"
                    + "先判断是咨询、报事还是投诉。需要落库时调用工单工具。"
                    + "不要编造不存在的工单号。回复简短、可执行。"
                    + "用户明确要转人工时，告诉他正在转接，不要自己承诺上门时间。";

    private final SessionStore store;
    private final TicketService tickets;
    private final SseHub sse;
    private final AssistProperties properties;
    private final QwenClient qwenClient;
    private final MockLlmClient mockClient;
    private final ObjectMapper mapper;
    private final ExecutorService workers = Executors.newCachedThreadPool();

    public ChatService(SessionStore store, TicketService tickets, SseHub sse,
                       AssistProperties properties, QwenClient qwenClient,
                       MockLlmClient mockClient, ObjectMapper mapper) {
        this.store = store;
        this.tickets = tickets;
        this.sse = sse;
        this.properties = properties;
        this.qwenClient = qwenClient;
        this.mockClient = mockClient;
        this.mapper = mapper;
    }

    public ChatSession onUserMessage(String userId, String userName, String content, String channel) {
        ChatSession session = store.getOrCreate(userId, userName);
        session.add("user", content, channel);
        sse.broadcast(session);

        if (isHandoff(content)) {
            session.setMode(ChatSession.MODE_HUMAN);
            session.add("assistant", "已为您转接人工客服，请稍候，同事会接过这场对话。", "system");
            sse.broadcast(session);
            return session;
        }

        if (ChatSession.MODE_HUMAN.equals(session.getMode())) {
            return session;
        }

        session.enqueue(workers, new Runnable() {
            public void run() {
                replyByAi(session);
            }
        });
        return session;
    }

    public ChatSession takeover(String userId) {
        ChatSession session = require(userId);
        session.setMode(ChatSession.MODE_HUMAN);
        session.add("system", "坐席已接管，后续由人工回复。", "agent");
        session.clearUnread();
        sse.broadcast(session);
        return session;
    }

    public ChatSession release(String userId) {
        ChatSession session = require(userId);
        session.setMode(ChatSession.MODE_AI);
        session.add("system", "已交回智能助手，后续先由 AI 辅助回复。", "agent");
        sse.broadcast(session);
        return session;
    }

    public ChatSession agentReply(String userId, String content) {
        ChatSession session = require(userId);
        session.setMode(ChatSession.MODE_HUMAN);
        session.add("agent", content, "agent");
        sse.broadcast(session);
        return session;
    }

    public String llmName() {
        return client().name();
    }

    public boolean liveModel() {
        return properties.getQwen().hasKey();
    }

    @PreDestroy
    public void shutdown() {
        workers.shutdownNow();
    }

    void replyByAi(ChatSession session) {
        try {
            List<Map<String, Object>> messages = buildMessages(session);
            LlmClient client = client();
            for (int round = 0; round < 3; round++) {
                LlmResult result = client.chat(messages);
                if (!result.hasTools()) {
                    String text = result.getText().trim();
                    if (text.length() == 0) {
                        text = "我在，您可以说报修、投诉，或转人工。";
                    }
                    session.add("assistant", text, "ai");
                    sse.broadcast(session);
                    return;
                }
                Map<String, Object> assistant = new HashMap<String, Object>();
                assistant.put("role", "assistant");
                assistant.put("content", result.getText());
                assistant.put("tool_calls", toToolCallPayload(result));
                messages.add(assistant);
                for (LlmResult.ToolCall call : result.getToolCalls()) {
                    String output = runTool(session.getUserId(), call);
                    Map<String, Object> toolMsg = new HashMap<String, Object>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", call.id);
                    toolMsg.put("name", call.name);
                    toolMsg.put("content", output);
                    messages.add(toolMsg);
                }
            }
            session.add("assistant", "工具调用次数过多，已停止。您可以转人工继续处理。", "ai");
            sse.broadcast(session);
        } catch (Exception e) {
            log.warn("AI reply failed: {}", e.getMessage());
            session.add("assistant", "刚才处理失败了。您可以再发一次，或直接转人工。", "ai");
            sse.broadcast(session);
        }
    }

    String runTool(String userId, LlmResult.ToolCall call) {
        try {
            JsonNode args = mapper.readTree(call.arguments == null || call.arguments.length() == 0
                    ? "{}" : call.arguments);
            if ("create_ticket".equals(call.name)) {
                Ticket t = tickets.create(userId,
                        text(args, "type"),
                        text(args, "title"),
                        text(args, "detail"),
                        text(args, "level"));
                sse.send("ticket", t);
                return "已创建工单 " + t.getId() + "，类型=" + t.getType() + "，状态=" + t.getStatus();
            }
            if ("query_tickets".equals(call.name)) {
                List<Ticket> list = tickets.query(userId, text(args, "keyword"));
                if (list.isEmpty()) {
                    return "当前没有工单。";
                }
                StringBuilder sb = new StringBuilder();
                for (Ticket t : list) {
                    sb.append(t.getId()).append(" ").append(t.getType())
                            .append(" ").append(t.getStatus()).append(" ")
                            .append(t.getTitle()).append("\n");
                }
                return sb.toString();
            }
            if ("assign_ticket".equals(call.name)) {
                Ticket t = tickets.assign(text(args, "id"), text(args, "assignee"));
                sse.send("ticket", t);
                return "已分派 " + t.getId() + " 给 " + t.getAssignee();
            }
            if ("close_ticket".equals(call.name)) {
                Ticket t = tickets.close(text(args, "id"), text(args, "reason"));
                sse.send("ticket", t);
                return "已关闭 " + t.getId();
            }
            return "未知工具: " + call.name;
        } catch (Exception e) {
            return "工具失败: " + e.getMessage();
        }
    }

    private List<Map<String, Object>> buildMessages(ChatSession session) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> sys = new HashMap<String, Object>();
        sys.put("role", "system");
        sys.put("content", SYSTEM_PROMPT);
        list.add(sys);
        List<ChatMessage> history = session.snapshot();
        int from = Math.max(0, history.size() - 12);
        for (int i = from; i < history.size(); i++) {
            ChatMessage m = history.get(i);
            if ("system".equals(m.getRole())) {
                continue;
            }
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("role", toLlmRole(m.getRole()));
            item.put("content", m.getContent());
            list.add(item);
        }
        return list;
    }

    private String toLlmRole(String role) {
        if ("agent".equals(role)) {
            return "assistant";
        }
        if ("assistant".equals(role)) {
            return "assistant";
        }
        return "user";
    }

    private List<Map<String, Object>> toToolCallPayload(LlmResult result) {
        List<Map<String, Object>> calls = new ArrayList<Map<String, Object>>();
        for (LlmResult.ToolCall call : result.getToolCalls()) {
            Map<String, Object> fn = new HashMap<String, Object>();
            fn.put("name", call.name);
            fn.put("arguments", call.arguments);
            Map<String, Object> one = new HashMap<String, Object>();
            one.put("id", call.id);
            one.put("type", "function");
            one.put("function", fn);
            calls.add(one);
        }
        return calls;
    }

    private boolean isHandoff(String content) {
        return content != null && (
                content.indexOf("转人工") >= 0
                        || content.indexOf("人工客服") >= 0
                        || content.indexOf("找人工") >= 0);
    }

    private ChatSession require(String userId) {
        ChatSession session = store.get(userId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在: " + userId);
        }
        return session;
    }

    private LlmClient client() {
        return properties.getQwen().hasKey() ? qwenClient : mockClient;
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? "" : v.asText("");
    }
}
