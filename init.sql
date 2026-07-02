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
    avatar      MEDIUMTEXT                           COMMENT '头像（Base64 Data URL，前端已压缩）',
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
('admin', '$2b$10$GU45IcZBLRvuMDdrgkOBnew1aW1zG1NExROp45yDx9JTq7v/8Q0AG', 'admin@example.com', 'ROLE_ADMIN', 1),
('operator', '$2b$10$GU45IcZBLRvuMDdrgkOBnew1aW1zG1NExROp45yDx9JTq7v/8Q0AG', 'operator@example.com', 'ROLE_OPERATOR', 1),
('viewer', '$2b$10$GU45IcZBLRvuMDdrgkOBnew1aW1zG1NExROp45yDx9JTq7v/8Q0AG', 'viewer@example.com', 'ROLE_VIEWER', 1);

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
    user_id        VARCHAR(64)                          COMMENT '数据归属用户ID（监控目标的创建者），NULL=系统/全局',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_type        (type),
    INDEX idx_operator    (operator),
    INDEX idx_ip_address  (ip_address),
    INDEX idx_user_id     (user_id),
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
    message       VARCHAR(512)                         COMMENT '告警消息',
    ip_address    VARCHAR(45)                          COMMENT '关联IP',
    port          INT                                  COMMENT '关联端口',
    details       TEXT                                 COMMENT '详细信息（JSON）',
    handler       VARCHAR(64)                          COMMENT '处理人',
    handle_time   DATETIME                             COMMENT '处理时间',
    handle_remark VARCHAR(512)                         COMMENT '处理备注',
    user_id       VARCHAR(64)                          COMMENT '数据归属用户ID（监控目标的创建者），NULL=系统/全局',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_type        (type),
    INDEX idx_level       (level),
    INDEX idx_status      (status),
    INDEX idx_ip_address  (ip_address),
    INDEX idx_user_id     (user_id),
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
    user_id         VARCHAR(64)                            COMMENT '数据归属用户ID（监控目标的创建者），NULL=系统/全局',
    create_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_ip_address (ip_address),
    INDEX idx_status     (status),
    INDEX idx_severity   (severity),
    INDEX idx_user_id    (user_id),
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
-- 7. 报告表（保留兼容性）
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

-- ------------------------------------------------------------
-- 8. 监控目标表（新增）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS monitored_target (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    ip_address  VARCHAR(64)  NOT NULL                COMMENT '监控目标IP',
    status      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1-监控中，0-已停止',
    user_id     VARCHAR(64)                          COMMENT '创建用户ID',
    agent_token VARCHAR(64)                          COMMENT '采集探针上报鉴权 token',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ip_user (ip_address, user_id),
    INDEX idx_status (status),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监控目标表';

-- ------------------------------------------------------------
-- 9. 指标快照表（新增，每30秒轮询一次）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS metrics_snapshot (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    ip_address        VARCHAR(64)  NOT NULL                COMMENT '监控目标IP',
    half_open_conns   INT          NOT NULL DEFAULT 0      COMMENT '半开连接数',
    request_rate      INT          NOT NULL DEFAULT 0      COMMENT '请求速率（次/分钟）',
    avg_conn_duration BIGINT       NOT NULL DEFAULT 0      COMMENT '平均连接时长（毫秒）',
    avg_packet_size   INT          NOT NULL DEFAULT 0      COMMENT '平均包大小（字节）',
    unique_source_ips INT          NOT NULL DEFAULT 0      COMMENT '唯一来源IP数',
    ai_verdict        TEXT                                 COMMENT 'DeepSeek分析结果（JSON）',
    is_attack         TINYINT      NOT NULL DEFAULT 0      COMMENT '是否攻击：0-否，1-是',
    severity          VARCHAR(20)                          COMMENT '严重程度：low/medium/high/critical',
    user_id           VARCHAR(64)                          COMMENT '数据归属用户ID（监控目标的创建者），NULL=系统/全局',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_ip_time (ip_address, create_time),
    INDEX idx_is_attack (is_attack),
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标快照表';

-- ------------------------------------------------------------
-- 10. 微信用户表（新增）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wx_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    openid      VARCHAR(128) NOT NULL                COMMENT '微信openid',
    nickname    VARCHAR(64)                          COMMENT '微信昵称',
    avatar_url  VARCHAR(512)                         COMMENT '头像URL',
    user_id     BIGINT                               COMMENT '关联sys_user.id',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_openid (openid),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信用户表';

-- ------------------------------------------------------------
-- 11. Agent 自动处置配置表（新增）
--     单行配置，控制感知-分析-决策-处置闭环的自动化行为
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS agent_config (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    auto_mode           TINYINT      NOT NULL DEFAULT 0      COMMENT '自动处置模式：0-关闭（仅建议），1-开启（自动封禁）',
    block_risk_score    INT          NOT NULL DEFAULT 80     COMMENT '自动封禁风险评分阈值（0-100）',
    block_severity      VARCHAR(20)  NOT NULL DEFAULT 'high' COMMENT '触发封禁的最低严重级别：low/medium/high/critical',
    block_min_confidence DOUBLE      NOT NULL DEFAULT 0.7    COMMENT '触发封禁的最低AI置信度（0-1）',
    auto_unblock_minutes INT         NOT NULL DEFAULT 0      COMMENT '自动解封时长（分钟），0=不自动解封',
    updated_by          VARCHAR(64)                          COMMENT '最后修改人',
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent自动处置配置表';

-- 默认配置：自动模式关闭，需管理员显式开启
INSERT INTO agent_config (id, auto_mode, block_risk_score, block_severity, block_min_confidence, auto_unblock_minutes, updated_by)
SELECT 1, 0, 80, 'high', 0.7, 0, 'system'
WHERE NOT EXISTS (SELECT 1 FROM agent_config WHERE id = 1);

-- ------------------------------------------------------------
-- 12. 封禁 IP 表（新增）
--     软封禁：仅记录状态，可由系统自动或人工解封
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS blocked_ip (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    ip_address    VARCHAR(64)  NOT NULL                COMMENT '被封禁IP',
    target_ip     VARCHAR(64)                          COMMENT '关联监控目标IP',
    status        TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1-封禁中，0-已解封',
    auto          TINYINT      NOT NULL DEFAULT 0      COMMENT '是否自动封禁：0-人工，1-自动',
    reason        VARCHAR(512)                         COMMENT '封禁原因',
    risk_score    DOUBLE                               COMMENT '触发时风险评分',
    severity      VARCHAR(20)                          COMMENT '触发时严重级别',
    operator      VARCHAR(64)                          COMMENT '操作人（自动为 agent）',
    block_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '封禁时间',
    unblock_time  DATETIME                             COMMENT '解封时间',
    unblock_by    VARCHAR(64)                          COMMENT '解封人',
    user_id       VARCHAR(64)                          COMMENT '数据归属用户ID（监控目标的创建者），NULL=系统/全局',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_ip_address (ip_address),
    INDEX idx_status     (status),
    INDEX idx_user_id    (user_id),
    INDEX idx_block_time (block_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='封禁IP表';

-- ------------------------------------------------------------
-- 迁移：为已存在的 sys_user 表补充 avatar 列（幂等）
-- MySQL 不支持 ADD COLUMN IF NOT EXISTS，用存储过程判断后再加
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS add_avatar_column;
DELIMITER //
CREATE PROCEDURE add_avatar_column()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_user'
          AND COLUMN_NAME = 'avatar'
    ) THEN
        ALTER TABLE sys_user ADD COLUMN avatar MEDIUMTEXT
            COMMENT '头像（Base64 Data URL，前端已压缩）' AFTER email;
    END IF;
END //
DELIMITER ;
CALL add_avatar_column();
DROP PROCEDURE IF EXISTS add_avatar_column;

-- ------------------------------------------------------------
-- 迁移：为 monitored_target 补充 agent_token 列（幂等）
--   目标主机上的采集探针(Agent)上报时携带此 token，
--   与「目标IP + token」匹配校验通过才接收数据，防止伪造上报。
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS add_agent_token_column;
DELIMITER //
CREATE PROCEDURE add_agent_token_column()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'monitored_target'
          AND COLUMN_NAME = 'agent_token'
    ) THEN
        ALTER TABLE monitored_target ADD COLUMN agent_token VARCHAR(64)
            COMMENT '采集探针上报鉴权 token' AFTER user_id;
    END IF;
END //
DELIMITER ;
CALL add_agent_token_column();
DROP PROCEDURE IF EXISTS add_agent_token_column;

-- ------------------------------------------------------------
-- 迁移：多用户数据隔离（幂等）
--   为各数据表补充 user_id 列，记录数据归属用户（监控目标创建者）。
--   普通用户仅能查看/删除自己 user_id 的数据；管理员可见全部。
--   历史数据 user_id 为 NULL，迁移后默认仅管理员可见。
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS add_user_id_column;
DELIMITER //
CREATE PROCEDURE add_user_id_column(IN tbl VARCHAR(64), IN after_col VARCHAR(64))
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tbl
          AND COLUMN_NAME = 'user_id'
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE ', tbl,
            ' ADD COLUMN user_id VARCHAR(64) NULL COMMENT ''数据归属用户ID'' AFTER ', after_col);
        PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
        SET @idx = CONCAT('ALTER TABLE ', tbl, ' ADD INDEX idx_user_id (user_id)');
        PREPARE stmt2 FROM @idx; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;
    END IF;
END //
DELIMITER ;
CALL add_user_id_column('alert', 'handle_remark');
CALL add_user_id_column('sys_log', 'os');
CALL add_user_id_column('attack_detection', 'resolved_at');
CALL add_user_id_column('metrics_snapshot', 'severity');
CALL add_user_id_column('blocked_ip', 'unblock_by');
DROP PROCEDURE IF EXISTS add_user_id_column;

-- ------------------------------------------------------------
-- 迁移：monitored_target 唯一键从 (ip_address) 改为 (ip_address, user_id)（幂等）
--   使同一台服务器 IP 可被多个账号各自独立监控，互不覆盖。
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS fix_target_unique_key;
DELIMITER //
CREATE PROCEDURE fix_target_unique_key()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'monitored_target'
          AND INDEX_NAME = 'uk_ip_address'
    ) THEN
        ALTER TABLE monitored_target DROP INDEX uk_ip_address;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'monitored_target'
          AND INDEX_NAME = 'uk_ip_user'
    ) THEN
        ALTER TABLE monitored_target ADD UNIQUE KEY uk_ip_user (ip_address, user_id);
    END IF;
END //
DELIMITER ;
CALL fix_target_unique_key();
DROP PROCEDURE IF EXISTS fix_target_unique_key;
