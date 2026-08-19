package com.zhangchong.assist.chat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;

public class ChatSession {

    public static final String MODE_AI = "AI";
    public static final String MODE_HUMAN = "HUMAN";

    private final String userId;
    private final String userName;
    private volatile String mode = MODE_AI;
    private volatile long updatedAt = System.currentTimeMillis();
    private volatile int unreadForAgent;
    private final List<ChatMessage> messages = new ArrayList<ChatMessage>();
    private final Object gate = new Object();
    private final Queue<Runnable> queue = new ArrayDeque<Runnable>();
    private boolean busy;

    public ChatSession(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
        touch();
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public int getUnreadForAgent() {
        return unreadForAgent;
    }

    public void clearUnread() {
        unreadForAgent = 0;
    }

    public List<ChatMessage> snapshot() {
        synchronized (messages) {
            return new ArrayList<ChatMessage>(messages);
        }
    }

    public ChatMessage add(String role, String content, String channel) {
        ChatMessage msg = ChatMessage.of(role, content, channel);
        synchronized (messages) {
            messages.add(msg);
        }
        if ("user".equals(role)) {
            unreadForAgent++;
        }
        touch();
        return msg;
    }

    public void enqueue(Executor executor, Runnable task) {
        synchronized (gate) {
            queue.add(task);
            if (!busy) {
                busy = true;
                executor.execute(new Runnable() {
                    public void run() {
                        drain();
                    }
                });
            }
        }
    }

    private void drain() {
        while (true) {
            Runnable next;
            synchronized (gate) {
                next = queue.poll();
                if (next == null) {
                    busy = false;
                    return;
                }
            }
            next.run();
        }
    }

    public SessionView toView() {
        SessionView v = new SessionView();
        v.userId = userId;
        v.userName = userName;
        v.mode = mode;
        v.updatedAt = updatedAt;
        v.unreadForAgent = unreadForAgent;
        List<ChatMessage> all = snapshot();
        v.messageCount = all.size();
        v.lastMessage = all.isEmpty() ? "" : all.get(all.size() - 1).getContent();
        v.messages = all;
        return v;
    }

    public SessionView toSummary() {
        SessionView v = toView();
        v.messages = Collections.emptyList();
        return v;
    }

    private void touch() {
        updatedAt = System.currentTimeMillis();
    }

    public static class SessionView {
        public String userId;
        public String userName;
        public String mode;
        public long updatedAt;
        public int unreadForAgent;
        public int messageCount;
        public String lastMessage;
        public List<ChatMessage> messages;
    }
}
