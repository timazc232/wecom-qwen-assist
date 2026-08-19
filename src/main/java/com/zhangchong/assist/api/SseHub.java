package com.zhangchong.assist.api;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.zhangchong.assist.chat.ChatSession;

@Component
public class SseHub {

    private static final Logger log = LoggerFactory.getLogger(SseHub.class);
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<SseEmitter>();

    public SseEmitter subscribe() {
        final SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        Runnable remove = new Runnable() {
            public void run() {
                emitters.remove(emitter);
            }
        };
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(new java.util.function.Consumer<Throwable>() {
            public void accept(Throwable ex) {
                emitters.remove(emitter);
            }
        });
        return emitter;
    }

    public void broadcast(ChatSession session) {
        send("session", session.toView());
    }

    public void send(String event, Object data) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(event).data(data));
            } catch (IOException e) {
                emitters.remove(emitter);
                log.debug("drop sse client: {}", e.getMessage());
            }
        }
    }
}
