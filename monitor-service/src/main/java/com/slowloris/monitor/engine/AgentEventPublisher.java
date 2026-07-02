package com.slowloris.monitor.engine;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Agent 实时事件推送中心（基于 SSE）。
 * 前端通过 {@code GET /monitor/stream} 订阅，闭环各阶段（分析完成、检出攻击、作出处置）
 * 实时广播，免去前端 30 秒轮询。所有事件同时已落库，SSE 断开不影响数据一致性。
 *
 * <p><b>多用户隔离：</b>每个订阅绑定订阅者的 userId 与是否管理员。带归属的事件
 * （analysis/disposition/block/unblock）仅推送给「数据归属用户本人」及「管理员」；
 * 全局事件（如 config）推送给所有订阅者。</p>
 */
@Slf4j
@Component
public class AgentEventPublisher {

    /** 一个订阅连接：emitter + 订阅者归属信息。 */
    private static class Subscriber {
        final SseEmitter emitter;
        final String userId;
        final boolean admin;
        Subscriber(SseEmitter emitter, String userId, boolean admin) {
            this.emitter = emitter;
            this.userId = userId;
            this.admin = admin;
        }
    }

    private final CopyOnWriteArrayList<Subscriber> subscribers = new CopyOnWriteArrayList<>();

    /** 默认无超时（0L 表示不超时），由客户端断开或服务端异常清理。 */
    public SseEmitter subscribe(String userId, boolean admin) {
        SseEmitter emitter = new SseEmitter(0L);
        Subscriber sub = new Subscriber(emitter, userId, admin);
        subscribers.add(sub);
        emitter.onCompletion(() -> subscribers.remove(sub));
        emitter.onTimeout(() -> subscribers.remove(sub));
        emitter.onError(e -> subscribers.remove(sub));
        try {
            emitter.send(SseEmitter.event().name("connected").data("{\"ok\":true}"));
        } catch (IOException e) {
            subscribers.remove(sub);
        }
        log.debug("SSE 订阅新增 user={} admin={}，当前连接数: {}", userId, admin, subscribers.size());
        return emitter;
    }

    /**
     * 推送带数据归属的事件：仅归属用户本人与管理员可收到。
     * ownerUserId 为 null（历史/系统数据）时只有管理员可收到。
     */
    public void broadcastToOwner(String event, Object payload, String ownerUserId) {
        if (subscribers.isEmpty()) return;
        String json = JSON.toJSONString(payload);
        for (Subscriber sub : subscribers) {
            if (sub.admin || Objects.equals(sub.userId, ownerUserId)) {
                send(sub, event, json);
            }
        }
    }

    /** 全局广播：所有订阅者均可收到（如配置变更）。 */
    public void broadcast(String event, Object payload) {
        if (subscribers.isEmpty()) return;
        String json = JSON.toJSONString(payload);
        for (Subscriber sub : subscribers) {
            send(sub, event, json);
        }
    }

    public void broadcast(String event, Map<String, Object> payload) {
        broadcast(event, (Object) payload);
    }

    private void send(Subscriber sub, String event, String json) {
        try {
            sub.emitter.send(SseEmitter.event().name(event).data(json));
        } catch (Exception e) {
            sub.emitter.complete();
            subscribers.remove(sub);
        }
    }

    public int connectionCount() {
        return subscribers.size();
    }
}
