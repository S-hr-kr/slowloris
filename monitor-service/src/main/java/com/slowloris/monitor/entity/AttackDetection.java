package com.slowloris.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 攻击检测/决策记录。每当 agent 判定为攻击并作出决策时落库一条。
 * 复用 init.sql 中已定义但此前未使用的 attack_detection 表。
 */
@Data
@TableName("attack_detection")
public class AttackDetection implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String ipAddress;

    /** 攻击类型：slowloris/other_ddos/... */
    private String type;

    /** 状态：active-处置中/已建议，blocked-已封禁，resolved-已解除 */
    private String status;

    private String severity;

    private Double confidence;

    private Double riskScore;

    /** AI 推理说明 */
    private String reasoning;

    /** 处置建议（JSON 数组） */
    private String recommendations;

    /** 详细信息（JSON） */
    private String details;

    private LocalDateTime detectedAt;

    private LocalDateTime resolvedAt;

    /** 数据归属用户ID（监控目标的创建者），NULL=系统/全局，仅管理员可见 */
    private String userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
