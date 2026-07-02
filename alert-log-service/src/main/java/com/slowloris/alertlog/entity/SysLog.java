package com.slowloris.alertlog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_log")
public class SysLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Integer type;
    private String operator;
    private String ipAddress;
    private String module;
    private String description;
    private String method;
    private String requestUrl;
    private String requestParams;
    private Integer statusCode;
    private Long executionTime;
    private String errorInfo;
    private String browser;
    private String os;
    /** 数据归属用户ID（监控目标的创建者），NULL=系统/全局，仅管理员可见 */
    private String userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
