package com.zhangchong.assist.chat;

public class ChatMessage {

    private String id;
    private String role;
    private String content;
    private String channel;
    private long time;

    public static ChatMessage of(String role, String content, String channel) {
        ChatMessage m = new ChatMessage();
        m.id = "m-" + Long.toHexString(System.nanoTime());
        m.role = role;
        m.content = content;
        m.channel = channel;
        m.time = System.currentTimeMillis();
        return m;
    }

    public String getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public String getChannel() {
        return channel;
    }

    public long getTime() {
        return time;
    }
}
