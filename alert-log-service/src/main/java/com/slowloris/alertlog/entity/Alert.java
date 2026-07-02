package com.slowloris.alertlog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("alert")
public class Alert implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String type;
    private Integer level;
    private Integer status;
    private String description;
    private String message;
    private String ipAddress;
    private Integer port;
    private String details;
    private String handler;
    private LocalDateTime handleTime;
    private String handleRemark;
    /** 数据归属用户ID（监控目标的创建者），NULL=系统/全局，仅管理员可见 */
    private String userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
