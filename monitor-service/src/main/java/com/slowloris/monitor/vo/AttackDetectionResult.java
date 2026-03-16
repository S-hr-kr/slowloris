package com.slowloris.monitor.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class AttackDetectionResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean isAttack;
    private String attackType;
    private String severity;
    private double confidence;
    private double riskScore;
    private String reasoning;
    private List<String> recommendations;
    private Map<String, Object> details;
}
