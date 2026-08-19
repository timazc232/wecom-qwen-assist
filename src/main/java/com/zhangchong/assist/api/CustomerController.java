package com.zhangchong.assist.api;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zhangchong.assist.chat.ChatService;
import com.zhangchong.assist.chat.ChatSession;
import com.zhangchong.assist.chat.SessionStore;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    private final ChatService chatService;
    private final SessionStore store;

    public CustomerController(ChatService chatService, SessionStore store) {
        this.chatService = chatService;
        this.store = store;
    }

    @PostMapping("/send")
    public ChatSession.SessionView send(@RequestBody Map<String, String> body) {
        String userId = required(body, "userId");
        String content = required(body, "content");
        String userName = body.get("userName");
        String channel = body.get("channel");
        if (channel == null || channel.trim().length() == 0) {
            channel = "wechat";
        }
        return chatService.onUserMessage(userId, userName, content, channel).toView();
    }

    @GetMapping("/{userId}")
    public ChatSession.SessionView one(@PathVariable String userId) {
        ChatSession session = store.get(userId);
        if (session == null) {
            return store.getOrCreate(userId, userId).toView();
        }
        return session.toView();
    }

    private String required(Map<String, String> body, String key) {
        String v = body.get(key);
        if (v == null || v.trim().length() == 0) {
            throw new IllegalArgumentException("缺少 " + key);
        }
        return v.trim();
    }
}
