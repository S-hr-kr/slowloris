package com.slowloris.monitor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "snmp")
public class SnmpConfig {

    /** 是否启用 SNMP 采集 */
    private boolean enabled = false;

    /** SNMP 请求超时（毫秒） */
    private int timeout = 2000;

    /** 重试次数 */
    private int retries = 1;

    /** SNMP v2c community 只读串 */
    private String community = "public";

    /** 目标 IP → SNMP 设备的映射 */
    private List<TargetEntry> targets = new ArrayList<>();

    @Data
    public static class TargetEntry {
        /** 被监控的目标 IP */
        private String targetIp;
        /** SNMP 设备地址 */
        private String device;
        /** SNMP 端口，默认 161 */
        private int port = 161;
    }
}
