package com.slowloris.monitor.service;

import com.slowloris.monitor.vo.AttackDetectionRequest;
import com.slowloris.monitor.vo.AttackDetectionResult;

public interface AttackDetectionService {
    AttackDetectionResult detectAttack(AttackDetectionRequest request);
}
