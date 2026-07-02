package com.slowloris.monitor.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class MetricsData {
    private String ipAddress;
    private int halfOpenConns;
    private int requestRate;
    private long avgConnDuration;
    private int avgPacketSize;
    private int uniqueSourceIps;
    private LocalDateTime timestamp;
    /** 本窗口内真实/模拟的来源 IP 候选集合，作为可疑 IP 溯源的事实依据 */
    private List<String> sourceIps = new ArrayList<>();
}
