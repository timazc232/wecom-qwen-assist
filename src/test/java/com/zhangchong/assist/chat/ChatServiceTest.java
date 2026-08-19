package com.zhangchong.assist.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private SessionStore store;

    @Test
    public void handoffSkipsAi() {
        ChatSession session = chatService.onUserMessage("u-handoff", "测试用户", "转人工", "wechat");
        assertEquals(ChatSession.MODE_HUMAN, session.getMode());
        assertTrue(session.snapshot().size() >= 2);
        assertEquals("assistant", session.snapshot().get(1).getRole());
    }

    @Test
    public void takeoverAndAgentReply() throws Exception {
        chatService.onUserMessage("u-take", "张三", "你好", "wechat");
        Thread.sleep(400);
        ChatSession taken = chatService.takeover("u-take");
        assertEquals(ChatSession.MODE_HUMAN, taken.getMode());
        chatService.agentReply("u-take", "我是坐席，马上帮您看。");
        ChatSession latest = store.get("u-take");
        String last = latest.snapshot().get(latest.snapshot().size() - 1).getContent();
        assertEquals("我是坐席，马上帮您看。", last);
    }
}
