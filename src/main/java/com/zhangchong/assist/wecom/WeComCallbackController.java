package com.zhangchong.assist.wecom;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zhangchong.assist.chat.ChatService;
import com.zhangchong.assist.chat.ChatSession;
import com.zhangchong.assist.config.AssistProperties;

/**
 * 企业微信/微信客服回调入口。
 * 生产环境需要按官方文档做签名校验和 AES 解密；这里保留 URL 验证和明文 JSON，方便本地演示。
 */
@RestController
@RequestMapping("/wecom")
public class WeComCallbackController {

    private static final Logger log = LoggerFactory.getLogger(WeComCallbackController.class);

    private final AssistProperties properties;
    private final ChatService chatService;

    public WeComCallbackController(AssistProperties properties, ChatService chatService) {
        this.properties = properties;
        this.chatService = chatService;
    }

    @GetMapping("/callback")
    public String verify(@RequestParam(value = "echostr", required = false) String echostr,
                         @RequestParam(value = "echoStr", required = false) String echoStr) {
        String echo = echostr != null ? echostr : echoStr;
        log.info("wecom url verify, token={}", properties.getWecom().getToken());
        return echo == null ? "ok" : echo;
    }

    @PostMapping("/callback")
    public Map<String, Object> inbound(@RequestBody Map<String, Object> body) {
        String userId = first(body, new String[]{"FromUserName", "from", "userId", "openid"});
        String content = extractContent(body);
        String userName = first(body, new String[]{"userName", "nickname"});
        String channel = first(body, new String[]{"channel"});
        if (channel.length() == 0) {
            channel = "wecom";
        }
        if (userId.length() == 0 || content.length() == 0) {
            Map<String, Object> err = new HashMap<String, Object>();
            err.put("ok", Boolean.FALSE);
            err.put("error", "缺少 FromUserName/content");
            return err;
        }
        ChatSession session = chatService.onUserMessage(userId, userName, content, channel);
        Map<String, Object> ok = new HashMap<String, Object>();
        ok.put("ok", Boolean.TRUE);
        ok.put("userId", session.getUserId());
        ok.put("mode", session.getMode());
        return ok;
    }

    private String extractContent(Map<String, Object> body) {
        String direct = first(body, new String[]{"Content", "content", "text"});
        if (direct.length() > 0) {
            return direct;
        }
        Object msg = body.get("text");
        if (msg instanceof Map) {
            Object c = ((Map<?, ?>) msg).get("content");
            return c == null ? "" : String.valueOf(c);
        }
        return "";
    }

    private String first(Map<String, Object> body, String[] keys) {
        for (int i = 0; i < keys.length; i++) {
            Object v = body.get(keys[i]);
            if (v != null && String.valueOf(v).trim().length() > 0) {
                return String.valueOf(v).trim();
            }
        }
        return "";
    }
}
