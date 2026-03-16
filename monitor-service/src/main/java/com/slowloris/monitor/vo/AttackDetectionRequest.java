package com.slowloris.monitor.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class AttackDetectionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String ipAddress;
    private Map<String, Object> trafficData;
    private Map<String, Object> connectionData;
}
