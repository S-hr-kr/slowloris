package com.slowloris.monitor.service;

import java.util.Map;

public interface WebSocketBroadcastService {
    void broadcastNewAttack(Map<String, Object> attack);
    void broadcastAttackUpdate(Map<String, Object> update);
    void broadcastAlertNew(Map<String, Object> alert);
    void broadcastPredictionUpdate(Map<String, Object> prediction);
}
