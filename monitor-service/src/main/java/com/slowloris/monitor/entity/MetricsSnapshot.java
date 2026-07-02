package com.slowloris.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("metrics_snapshot")
public class MetricsSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String ipAddress;
    private Integer halfOpenConns;
    private Integer requestRate;
    private Long avgConnDuration;
    private Integer avgPacketSize;
    private Integer uniqueSourceIps;
    private String aiVerdict;
    private Integer isAttack;
    private String severity;
    /** 数据归属用户ID（监控目标的创建者），NULL=系统/全局，仅管理员可见 */
    private String userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
