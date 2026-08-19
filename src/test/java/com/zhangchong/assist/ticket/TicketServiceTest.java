package com.zhangchong.assist.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

public class TicketServiceTest {

    @Test
    public void createAssignAndClose() {
        TicketService svc = new TicketService();
        Ticket created = svc.create("wx-chen", "报事", "卫生间漏水", "顶楼渗水", "中");
        assertTrue(created.getId().startsWith("T"));
        assertEquals("待受理", created.getStatus());

        Ticket assigned = svc.assign(created.getId(), "管家-王工");
        assertEquals("处理中", assigned.getStatus());
        assertEquals("管家-王工", assigned.getAssignee());

        Ticket closed = svc.close(created.getId(), "已修好");
        assertEquals("已关闭", closed.getStatus());
        assertTrue(closed.getDetail().contains("已修好"));
    }

    @Test
    public void queryByUser() {
        TicketService svc = new TicketService();
        svc.create("a", "投诉", "态度差", "前台态度差", "高");
        svc.create("b", "报事", "门禁坏了", "门禁刷不开", "中");
        List<Ticket> onlyA = svc.query("a", null);
        assertEquals(1, onlyA.size());
        assertEquals("投诉", onlyA.get(0).getType());
    }
}
