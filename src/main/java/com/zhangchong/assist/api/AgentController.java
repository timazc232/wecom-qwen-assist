package com.zhangchong.assist.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.zhangchong.assist.chat.ChatService;
import com.zhangchong.assist.chat.ChatSession;
import com.zhangchong.assist.chat.SessionStore;
import com.zhangchong.assist.ticket.Ticket;
import com.zhangchong.assist.ticket.TicketService;

@RestController
@RequestMapping("/api")
public class AgentController {

    private final ChatService chatService;
    private final SessionStore store;
    private final TicketService tickets;
    private final SseHub sse;

    public AgentController(ChatService chatService, SessionStore store,
                           TicketService tickets, SseHub sse) {
        this.chatService = chatService;
        this.store = store;
        this.tickets = tickets;
        this.sse = sse;
    }

    @GetMapping("/meta")
    public Map<String, Object> meta() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("llm", chatService.llmName());
        m.put("liveModel", Boolean.valueOf(chatService.liveModel()));
        return m;
    }

    @GetMapping("/sessions")
    public List<ChatSession.SessionView> sessions() {
        List<ChatSession.SessionView> out = new ArrayList<ChatSession.SessionView>();
        for (ChatSession s : store.listRecent()) {
            out.add(s.toSummary());
        }
        return out;
    }

    @GetMapping("/sessions/{userId}")
    public ChatSession.SessionView session(@PathVariable String userId) {
        ChatSession s = store.get(userId);
        if (s == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        s.clearUnread();
        return s.toView();
    }

    @PostMapping("/sessions/{userId}/takeover")
    public ChatSession.SessionView takeover(@PathVariable String userId) {
        return chatService.takeover(userId).toView();
    }

    @PostMapping("/sessions/{userId}/release")
    public ChatSession.SessionView release(@PathVariable String userId) {
        return chatService.release(userId).toView();
    }

    @PostMapping("/sessions/{userId}/reply")
    public ChatSession.SessionView reply(@PathVariable String userId,
                                         @RequestBody Map<String, String> body) {
        return chatService.agentReply(userId, body.get("content")).toView();
    }

    @GetMapping("/tickets")
    public List<Ticket> tickets() {
        return tickets.all();
    }

    @GetMapping("/events")
    public SseEmitter events() {
        return sse.subscribe();
    }
}
