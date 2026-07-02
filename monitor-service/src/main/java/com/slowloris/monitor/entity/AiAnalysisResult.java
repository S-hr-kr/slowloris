package com.slowloris.monitor.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class AiAnalysisResult {
    @JsonProperty("isAttack")
    private boolean isAttack;
    private String attackType;
    private String severity;
    private double confidence;
    private double riskScore;
    private String reasoning;
    private List<String> recommendations;
    private List<String> suspiciousIps;
}
