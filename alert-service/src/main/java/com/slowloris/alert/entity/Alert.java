package com.slowloris.alert.entity;

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

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 告警类型
     */
    private String type;

    /**
     * 告警级别：1-信息，2-警告，3-错误，4-严重
     */
    private Integer level;

    /**
     * 告警状态：0-未处理，1-已处理，2-已忽略
     */
    private Integer status;

    /**
     * 告警描述
     */
    private String description;

    /**
     * 关联IP地址
     */
    private String ipAddress;

    /**
     * 关联端口
     */
    private Integer port;

    /**
     * 详细信息
     */
    private String details;

    /**
     * 处理人
     */
    private String handler;

    /**
     * 处理时间
     */
    private LocalDateTime handleTime;

    /**
     * 处理备注
     */
    private String handleRemark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}