package com.zhangchong.assist.llm;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class MockLlmClient implements LlmClient {

    public LlmResult chat(List<Map<String, Object>> messages) {
        String lastUser = lastUserText(messages);
        boolean afterTool = hasToolMessage(messages);

        LlmResult result = new LlmResult();
        if (afterTool) {
            result.setText("已经帮您记下来了。后续会有同事跟进，您也可以随时说「转人工」让坐席接手。");
            return result;
        }

        if (containsAny(lastUser, new String[]{"投诉", "态度", "乱收费", "收费不对"})) {
            result.getToolCalls().add(new LlmResult.ToolCall(
                    "call-complaint", "create_ticket", ticketArgs("投诉", lastUser, "高")));
            return result;
        }
        if (containsAny(lastUser, new String[]{"报修", "漏水", "门禁", "停车", "电梯", "水电"})) {
            result.getToolCalls().add(new LlmResult.ToolCall(
                    "call-repair", "create_ticket", ticketArgs("报事", lastUser, "中")));
            return result;
        }
        if (containsAny(lastUser, new String[]{"查工单", "工单呢", "进度", "查一下"})) {
            result.getToolCalls().add(new LlmResult.ToolCall(
                    "call-query", "query_tickets", "{\"keyword\":\"\"}"));
            return result;
        }

        if (lastUser.trim().length() == 0) {
            result.setText("您好，我是物业智能助手。可以说报修、投诉，或直接转人工。");
        } else {
            result.setText("收到：「" + lastUser + "」。我可以帮您报修、登记投诉或查询工单；需要同事处理请说「转人工」。");
        }
        return result;
    }

    public String name() {
        return "mock-qwen";
    }

    private String lastUserText(List<Map<String, Object>> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Map<String, Object> m = messages.get(i);
            if ("user".equals(String.valueOf(m.get("role")))) {
                Object c = m.get("content");
                return c == null ? "" : String.valueOf(c);
            }
        }
        return "";
    }

    private boolean hasToolMessage(List<Map<String, Object>> messages) {
        for (Map<String, Object> m : messages) {
            if ("tool".equals(String.valueOf(m.get("role")))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String text, String[] keys) {
        for (int i = 0; i < keys.length; i++) {
            if (text.indexOf(keys[i]) >= 0) {
                return true;
            }
        }
        return false;
    }

    private String ticketArgs(String type, String detail, String level) {
        ObjectNode n = JsonNodeFactory.instance.objectNode();
        n.put("type", type);
        n.put("title", detail.length() > 24 ? detail.substring(0, 24) : detail);
        n.put("detail", detail);
        n.put("level", level);
        return n.toString();
    }
}
