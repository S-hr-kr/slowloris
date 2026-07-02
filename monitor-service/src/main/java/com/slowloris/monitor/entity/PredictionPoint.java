package com.slowloris.monitor.entity;

import lombok.Data;

@Data
public class PredictionPoint {
    private String time;
    private double probability;
}
