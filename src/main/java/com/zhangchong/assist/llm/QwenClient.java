package com.zhangchong.assist.llm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangchong.assist.config.AssistProperties;

@Component
public class QwenClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(QwenClient.class);

    private final AssistProperties properties;
    private final ObjectMapper mapper;
    private final RestTemplate restTemplate;

    public QwenClient(AssistProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getQwen().getTimeoutMs());
        factory.setReadTimeout(properties.getQwen().getTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    public LlmResult chat(List<Map<String, Object>> messages) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("model", properties.getQwen().getModel());
        body.put("messages", messages);
        body.put("tools", tools());
        body.put("temperature", 0.2);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getQwen().getApiKey());

        String url = trimSlash(properties.getQwen().getBaseUrl()) + "/chat/completions";
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(
                    url, new HttpEntity<Map<String, Object>>(body, headers), String.class);
            return parse(resp.getBody());
        } catch (Exception e) {
            log.warn("Qwen call failed: {}", e.getMessage());
            LlmResult fallback = new LlmResult();
            fallback.setText("模型暂时不可用，请稍后重试，或直接说「转人工」。");
            return fallback;
        }
    }

    public String name() {
        return properties.getQwen().getModel();
    }

    LlmResult parse(String json) throws Exception {
        LlmResult result = new LlmResult();
        JsonNode root = mapper.readTree(json);
        JsonNode message = root.path("choices").path(0).path("message");
        result.setText(textOrEmpty(message.path("content")));
        JsonNode tools = message.path("tool_calls");
        if (tools.isArray()) {
            for (JsonNode t : tools) {
                result.getToolCalls().add(new LlmResult.ToolCall(
                        textOrEmpty(t.path("id")),
                        textOrEmpty(t.path("function").path("name")),
                        textOrEmpty(t.path("function").path("arguments"))));
            }
        }
        return result;
    }

    private List<Map<String, Object>> tools() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        list.add(fn("create_ticket", "创建投诉或报事工单",
                params("type", "投诉或报事", "title", "标题", "detail", "详情", "level", "高/中/低")));
        list.add(fn("query_tickets", "查询当前用户工单",
                params("keyword", "可选关键词")));
        list.add(fn("assign_ticket", "分派工单给处理人",
                params("id", "工单号", "assignee", "处理人")));
        list.add(fn("close_ticket", "关闭工单",
                params("id", "工单号", "reason", "关单原因")));
        return list;
    }

    private Map<String, Object> fn(String name, String desc, Map<String, Object> params) {
        Map<String, Object> fn = new HashMap<String, Object>();
        fn.put("name", name);
        fn.put("description", desc);
        fn.put("parameters", params);
        Map<String, Object> tool = new HashMap<String, Object>();
        tool.put("type", "function");
        tool.put("function", fn);
        return tool;
    }

    private Map<String, Object> params(String... kv) {
        Map<String, Object> properties = new HashMap<String, Object>();
        List<String> required = new ArrayList<String>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Map<String, Object> p = new HashMap<String, Object>();
            p.put("type", "string");
            p.put("description", kv[i + 1]);
            properties.put(kv[i], p);
            required.add(kv[i]);
        }
        Map<String, Object> schema = new HashMap<String, Object>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }

    private String textOrEmpty(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? "" : node.asText("");
    }

    private String trimSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
