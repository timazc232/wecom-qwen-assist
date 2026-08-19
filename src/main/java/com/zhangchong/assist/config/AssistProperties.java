package com.zhangchong.assist.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "assist")
public class AssistProperties {

    private final Qwen qwen = new Qwen();
    private final Wecom wecom = new Wecom();

    public Qwen getQwen() {
        return qwen;
    }

    public Wecom getWecom() {
        return wecom;
    }

    public static class Qwen {
        private String apiKey = "";
        private String model = "qwen-plus";
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        private int timeoutMs = 20000;

        public boolean hasKey() {
            return apiKey != null && apiKey.trim().length() > 0;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    public static class Wecom {
        private String token = "demo-token";

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
