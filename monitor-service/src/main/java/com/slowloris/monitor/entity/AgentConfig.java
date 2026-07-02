package com.slowloris.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Agent 自动处置配置（单行配置，id 固定为 1）。
 * 控制「感知-分析-决策-处置」闭环中自动封禁的触发条件。
 */
@Data
@TableName("agent_config")
public class AgentConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 自动处置模式：0-关闭（仅生成建议），1-开启（满足条件自动软封禁） */
    private Integer autoMode;

    /** 自动封禁风险评分阈值（0-100） */
    private Integer blockRiskScore;

    /** 触发封禁的最低严重级别：low/medium/high/critical */
    private String blockSeverity;

    /** 触发封禁的最低 AI 置信度（0-1） */
    private Double blockMinConfidence;

    /** 自动解封时长（分钟），0 表示不自动解封 */
    private Integer autoUnblockMinutes;

    private String updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
