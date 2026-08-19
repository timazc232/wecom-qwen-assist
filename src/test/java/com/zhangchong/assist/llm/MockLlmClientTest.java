package com.zhangchong.assist.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class MockLlmClientTest {

    @Test
    public void repairCreatesTicketTool() {
        MockLlmClient client = new MockLlmClient();
        LlmResult result = client.chat(user("卫生间漏水，帮忙报修"));
        assertEquals(1, result.getToolCalls().size());
        assertEquals("create_ticket", result.getToolCalls().get(0).name);
        assertTrue(result.getToolCalls().get(0).arguments.contains("报事"));
    }

    @Test
    public void afterToolReturnsText() {
        MockLlmClient client = new MockLlmClient();
        List<Map<String, Object>> msgs = user("报修");
        Map<String, Object> tool = new HashMap<String, Object>();
        tool.put("role", "tool");
        tool.put("content", "已创建工单 T1001");
        msgs.add(tool);
        LlmResult result = client.chat(msgs);
        assertTrue(result.getText().length() > 0);
        assertTrue(result.getToolCalls().isEmpty());
    }

    private List<Map<String, Object>> user(String text) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("role", "user");
        m.put("content", text);
        list.add(m);
        return list;
    }
}
