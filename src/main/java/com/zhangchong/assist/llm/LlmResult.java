package com.zhangchong.assist.llm;

import java.util.ArrayList;
import java.util.List;

public class LlmResult {

    private String text = "";
    private final List<ToolCall> toolCalls = new ArrayList<ToolCall>();

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public boolean hasTools() {
        return !toolCalls.isEmpty();
    }

    public static class ToolCall {
        public String id;
        public String name;
        public String arguments;

        public ToolCall() {
        }

        public ToolCall(String id, String name, String arguments) {
            this.id = id;
            this.name = name;
            this.arguments = arguments;
        }
    }
}
