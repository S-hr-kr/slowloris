package com.slowloris.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 软封禁 IP 记录。仅记录封禁状态，不直接操作系统防火墙；可由 agent 自动或人工解封。
 */
@Data
@TableName("blocked_ip")
public class BlockedIp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 被封禁的来源 IP */
    private String ipAddress;

    /** 关联的监控目标 IP */
    private String targetIp;

    /** 状态：1-封禁中，0-已解封 */
    private Integer status;

    /** 是否自动封禁：0-人工，1-自动（agent 决策） */
    private Integer auto;

    private String reason;

    private Double riskScore;

    private String severity;

    /** 操作人，自动封禁时为 "agent" */
    private String operator;

    private LocalDateTime blockTime;

    private LocalDateTime unblockTime;

    private String unblockBy;

    /** 数据归属用户ID（监控目标的创建者），NULL=系统/全局，仅管理员可见 */
    private String userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
