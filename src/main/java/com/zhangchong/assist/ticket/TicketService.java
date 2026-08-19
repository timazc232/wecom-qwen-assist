package com.zhangchong.assist.ticket;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

@Service
public class TicketService {

    private final ConcurrentHashMap<String, Ticket> tickets = new ConcurrentHashMap<String, Ticket>();
    private final AtomicInteger seq = new AtomicInteger(1000);

    public Ticket create(String userId, String type, String title, String detail, String level) {
        Ticket t = new Ticket();
        t.setId("T" + seq.incrementAndGet());
        t.setUserId(userId);
        t.setType(emptyTo(type, "报事"));
        t.setTitle(emptyTo(title, "未命名事件"));
        t.setDetail(emptyTo(detail, title));
        t.setLevel(emptyTo(level, "中"));
        t.setStatus("待受理");
        t.setAssignee("");
        long now = System.currentTimeMillis();
        t.setCreatedAt(now);
        t.setUpdatedAt(now);
        tickets.put(t.getId(), t);
        return t;
    }

    public Ticket assign(String id, String assignee) {
        Ticket t = require(id);
        t.setAssignee(assignee);
        t.setStatus("处理中");
        t.setUpdatedAt(System.currentTimeMillis());
        return t;
    }

    public Ticket close(String id, String reason) {
        Ticket t = require(id);
        t.setStatus("已关闭");
        if (reason != null && reason.trim().length() > 0) {
            t.setDetail(t.getDetail() + " | 关单：" + reason);
        }
        t.setUpdatedAt(System.currentTimeMillis());
        return t;
    }

    public List<Ticket> query(String userId, String keyword) {
        List<Ticket> out = new ArrayList<Ticket>();
        for (Ticket t : tickets.values()) {
            if (userId != null && userId.trim().length() > 0 && !userId.equals(t.getUserId())) {
                continue;
            }
            if (keyword != null && keyword.trim().length() > 0) {
                String k = keyword.toLowerCase();
                String blob = (t.getId() + t.getTitle() + t.getDetail() + t.getType()).toLowerCase();
                if (blob.indexOf(k) < 0) {
                    continue;
                }
            }
            out.add(t);
        }
        return out;
    }

    public List<Ticket> all() {
        return new ArrayList<Ticket>(tickets.values());
    }

    private Ticket require(String id) {
        Ticket t = tickets.get(id);
        if (t == null) {
            throw new IllegalArgumentException("工单不存在: " + id);
        }
        return t;
    }

    private String emptyTo(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value.trim();
    }
}
