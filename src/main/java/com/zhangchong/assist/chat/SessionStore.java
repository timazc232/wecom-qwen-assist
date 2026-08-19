package com.zhangchong.assist.chat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class SessionStore {

    private final ConcurrentHashMap<String, ChatSession> sessions = new ConcurrentHashMap<String, ChatSession>();

    public ChatSession getOrCreate(String userId, String userName) {
        ChatSession existing = sessions.get(userId);
        if (existing != null) {
            return existing;
        }
        String name = userName == null || userName.trim().length() == 0 ? userId : userName;
        ChatSession created = new ChatSession(userId, name);
        ChatSession raced = sessions.putIfAbsent(userId, created);
        return raced == null ? created : raced;
    }

    public ChatSession get(String userId) {
        return sessions.get(userId);
    }

    public List<ChatSession> listRecent() {
        List<ChatSession> list = new ArrayList<ChatSession>(sessions.values());
        CollectionsSortByUpdated(list);
        return list;
    }

    private void CollectionsSortByUpdated(List<ChatSession> list) {
        java.util.Collections.sort(list, new Comparator<ChatSession>() {
            public int compare(ChatSession a, ChatSession b) {
                return Long.compare(b.getUpdatedAt(), a.getUpdatedAt());
            }
        });
    }
}
