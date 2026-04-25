package com.slowloris.monitor.service.impl;

import com.slowloris.monitor.service.WebSocketBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketBroadcastServiceImpl implements WebSocketBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcastNewAttack(Map<String, Object> attack) {
        try {
            messagingTemplate.convertAndSend("/topic/attacks/new", attack);
        } catch (Exception e) {
            log.warn("WebSocket broadcast failed for /topic/attacks/new: {}", e.getMessage());
        }
    }

    @Override
    public void broadcastAttackUpdate(Map<String, Object> update) {
        try {
            messagingTemplate.convertAndSend("/topic/attacks/update", update);
        } catch (Exception e) {
            log.warn("WebSocket broadcast failed for /topic/attacks/update: {}", e.getMessage());
        }
    }

    @Override
    public void broadcastAlertNew(Map<String, Object> alert) {
        try {
            messagingTemplate.convertAndSend("/topic/alerts/new", alert);
        } catch (Exception e) {
            log.warn("WebSocket broadcast failed for /topic/alerts/new: {}", e.getMessage());
        }
    }

    @Override
    public void broadcastPredictionUpdate(Map<String, Object> prediction) {
        try {
            messagingTemplate.convertAndSend("/topic/prediction/update", prediction);
        } catch (Exception e) {
            log.warn("WebSocket broadcast failed for /topic/prediction/update: {}", e.getMessage());
        }
    }
}
