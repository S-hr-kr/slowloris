-- ============================================================
-- 伺"机"守护 · 智能感知与决策系统
-- 数据库初始化脚本
-- ============================================================

CREATE DATABASE IF NOT EXISTS slowloris DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE slowloris;

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(64)  NOT NULL                COMMENT '用户名',
    password    VARCHAR(255) NOT NULL                COMMENT '密码（BCrypt）',
    email       VARCHAR(128)                         COMMENT '邮箱',
    roles       VARCHAR(128) NOT NULL DEFAULT 'ROLE_VIEWER' COMMENT '角色，逗号分隔',
    status      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：0-禁用，1-启用',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除，1-已删除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 默认管理员账号（密码：Admin@123456，BCrypt加密）
INSERT INTO sys_user (username, password, email, roles, status) VALUES
('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'admin@example.com', 'ROLE_ADMIN', 1),
('operator', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'operator@example.com', 'ROLE_OPERATOR', 1),
('viewer', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'viewer@example.com', 'ROLE_VIEWER', 1);

-- ------------------------------------------------------------
-- 2. 系统日志表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    type           TINYINT      NOT NULL DEFAULT 1      COMMENT '日志类型：1-操作，2-访问，3-错误',
    operator       VARCHAR(64)                          COMMENT '操作人',
    ip_address     VARCHAR(45)                          COMMENT '操作IP',
    module         VARCHAR(64)                          COMMENT '操作模块',
    description    VARCHAR(512)                         COMMENT '操作描述',
    method         VARCHAR(16)                          COMMENT '请求方法',
    request_url    VARCHAR(512)                         COMMENT '请求URL',
    request_params TEXT                                 COMMENT '请求参数',
    status_code    INT                                  COMMENT '响应状态码',
    execution_time BIGINT                               COMMENT '执行时间（毫秒）',
    error_info     TEXT                                 COMMENT '错误信息',
    browser        VARCHAR(128)                         COMMENT '浏览器',
    os             VARCHAR(64)                          COMMENT '操作系统',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_type        (type),
    INDEX idx_operator    (operator),
    INDEX idx_ip_address  (ip_address),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统日志表';

-- ------------------------------------------------------------
-- 3. 告警表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS alert (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    type          VARCHAR(64)  NOT NULL                COMMENT '告警类型',
    level         TINYINT      NOT NULL DEFAULT 1      COMMENT '告警级别：1-信息，2-警告，3-错误，4-严重',
    status        TINYINT      NOT NULL DEFAULT 0      COMMENT '状态：0-未处理，1-已处理，2-已忽略',
    description   VARCHAR(512)                         COMMENT '告警描述',
    ip_address    VARCHAR(45)                          COMMENT '关联IP',
    port          INT                                  COMMENT '关联端口',
    details       TEXT                                 COMMENT '详细信息（JSON）',
    handler       VARCHAR(64)                          COMMENT '处理人',
    handle_time   DATETIME                             COMMENT '处理时间',
    handle_remark VARCHAR(512)                         COMMENT '处理备注',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_type        (type),
    INDEX idx_level       (level),
    INDEX idx_status      (status),
    INDEX idx_ip_address  (ip_address),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警表';

-- ------------------------------------------------------------
-- 4. IP 监控表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ip_monitor (
    id               BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'ID',
    ip_address       VARCHAR(45) NOT NULL                COMMENT 'IP地址',
    access_count     INT         NOT NULL DEFAULT 0      COMMENT '访问次数',
    last_access_time DATETIME                            COMMENT '最近访问时间',
    traffic_size     BIGINT      NOT NULL DEFAULT 0      COMMENT '流量大小（字节）',
    status           TINYINT     NOT NULL DEFAULT 0      COMMENT '状态：0-正常，1-可疑，2-异常',
    country          VARCHAR(64)                         COMMENT '国家',
    city             VARCHAR(64)                         COMMENT '城市',
    isp              VARCHAR(128)                        COMMENT '运营商',
    create_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ip_address (ip_address),
    INDEX idx_status      (status),
    INDEX idx_access_count (access_count),
    INDEX idx_last_access  (last_access_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IP监控表';

-- ------------------------------------------------------------
-- 5. 攻击检测记录表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS attack_detection (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'ID',
    ip_address      VARCHAR(45)    NOT NULL                COMMENT '来源IP',
    type            VARCHAR(64)                            COMMENT '攻击类型',
    status          VARCHAR(20)    NOT NULL DEFAULT 'active' COMMENT '状态：active/blocked/resolved',
    severity        VARCHAR(20)                            COMMENT '严重程度：low/medium/high/critical',
    confidence      DOUBLE                                 COMMENT '置信度（0-1）',
    risk_score      DOUBLE                                 COMMENT '风险分数（0-100）',
    reasoning       TEXT                                   COMMENT 'AI推理说明',
    recommendations TEXT                                   COMMENT '处置建议（JSON）',
    details         TEXT                                   COMMENT '详细信息（JSON）',
    detected_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',
    resolved_at     DATETIME                               COMMENT '处理时间',
    create_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_ip_address (ip_address),
    INDEX idx_status     (status),
    INDEX idx_severity   (severity),
    INDEX idx_detected_at (detected_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='攻击检测记录表';

-- ------------------------------------------------------------
-- 6. AI 预测历史表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS prediction_history (
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    ip_address         VARCHAR(45)  NOT NULL                COMMENT 'IP地址',
    prediction         VARCHAR(32)  NOT NULL                COMMENT '预测结果：攻击/正常',
    confidence         DOUBLE                               COMMENT '置信度（0-1）',
    risk_score         DOUBLE                               COMMENT '风险分数（0-100）',
    model              VARCHAR(64)                          COMMENT '使用模型',
    feature_importance TEXT                                 COMMENT '特征重要性（JSON）',
    details            TEXT                                 COMMENT '预测详情（JSON）',
    create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_ip_address  (ip_address),
    INDEX idx_prediction  (prediction),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI预测历史表';

-- ------------------------------------------------------------
-- 7. 报告表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS report (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    type          TINYINT      NOT NULL DEFAULT 1      COMMENT '报告类型：1-攻击检测，2-流量分析，3-系统状态',
    name          VARCHAR(128) NOT NULL                COMMENT '报告名称',
    description   VARCHAR(512)                         COMMENT '报告描述',
    content       LONGTEXT                             COMMENT '报告内容（JSON）',
    generator     VARCHAR(64)                          COMMENT '生成人',
    generate_time DATETIME                             COMMENT '生成时间',
    start_time    DATETIME                             COMMENT '统计开始时间',
    end_time      DATETIME                             COMMENT '统计结束时间',
    status        TINYINT      NOT NULL DEFAULT 0      COMMENT '状态：0-生成中，1-已完成，2-失败',
    file_path     VARCHAR(512)                         COMMENT '文件路径',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_type        (type),
    INDEX idx_status      (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告表';
