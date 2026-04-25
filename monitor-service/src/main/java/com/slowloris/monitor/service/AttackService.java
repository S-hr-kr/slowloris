package com.slowloris.monitor.service;

import com.slowloris.common.Result;

import java.util.Map;

public interface AttackService {
    Result<Map<String, Object>> getAttacks();
    Result<String> blockAttack(Long attackId);
}
