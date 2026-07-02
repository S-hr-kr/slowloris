package com.slowloris.monitor.service.impl;

import com.slowloris.monitor.config.SnmpConfig;
import com.slowloris.monitor.entity.MetricsData;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 SNMP v2c 的网络层指标采集器。
 * <p>
 * 通过查询目标主机上游网络设备（路由器/防火墙/交换机）的 SNMP 接口，
 * 获取流量速率、TCP 连接状态、丢包等网络层指标，用于补充或替代
 * 应用层网关流量上报，提高 DDoS 检测的覆盖面和准确度。
 * </p>
 *
 * <h3>采集的核心 OID</h3>
 * <ul>
 *   <li>ifInOctets / ifOutOctets —— 接口字节计数（计算速率）</li>
 *   <li>ifInUcastPkts / ifOutUcastPkts —— 接口单播包计数</li>
 *   <li>ifInDiscards —— 入站丢包数</li>
 *   <li>tcpCurrEstab —— 当前已建立的 TCP 连接数</li>
 *   <li>tcpAttemptFails —— TCP 连接失败次数</li>
 *   <li>tcpInSegs / tcpOutSegs —— TCP 段收发计数</li>
 * </ul>
 */
@Slf4j
@Service
public class SnmpMetricsCollector {

    @Autowired
    private SnmpConfig snmpConfig;

    /** 缓存上次采集值，用于计算速率（OID → 上次值） */
    private final Map<String, Long> lastValues = new ConcurrentHashMap<>();

    /** 上次采集时间戳（OID → 时间），用于速率分母 */
    private final Map<String, Long> lastTimes = new ConcurrentHashMap<>();

    // ==================== 标准 MIB-II OID ====================

    private static final String SYS_UPTIME         = "1.3.6.1.2.1.1.3.0";

    private static final String IF_IN_OCTETS       = "1.3.6.1.2.1.2.2.1.10";   // + .ifIndex
    private static final String IF_OUT_OCTETS      = "1.3.6.1.2.1.2.2.1.16";   // + .ifIndex
    private static final String IF_IN_UCAST_PKTS   = "1.3.6.1.2.1.2.2.1.11";   // + .ifIndex
    private static final String IF_OUT_UCAST_PKTS  = "1.3.6.1.2.1.2.2.1.17";   // + .ifIndex
    private static final String IF_IN_DISCARDS     = "1.3.6.1.2.1.2.2.1.13";   // + .ifIndex
    private static final String IF_OPER_STATUS     = "1.3.6.1.2.1.2.2.1.8";    // + .ifIndex

    private static final String TCP_CURR_ESTAB     = "1.3.6.1.2.1.6.9.0";
    private static final String TCP_ATTEMPT_FAILS  = "1.3.6.1.2.1.6.7.0";
    private static final String TCP_IN_SEGS        = "1.3.6.1.2.1.6.10.0";
    private static final String TCP_OUT_SEGS       = "1.3.6.1.2.1.6.11.0";

    /**
     * 对指定目标 IP 执行 SNMP 采集。
     *
     * @param targetIp 被监控的目标 IP
     * @return 采集到的指标数据，若 SNMP 未启用或无可匹配设备则返回 null
     */
    public MetricsData collect(String targetIp) {
        if (!snmpConfig.isEnabled()) {
            return null;
        }

        SnmpConfig.TargetEntry entry = findDevice(targetIp);
        if (entry == null) {
            log.debug("未找到 IP {} 对应的 SNMP 设备配置", targetIp);
            return null;
        }

        try {
            return doCollect(entry, targetIp);
        } catch (Exception e) {
            log.warn("SNMP 采集失败，target={}, device={}: {}", targetIp, entry.getDevice(), e.getMessage());
            return null;
        }
    }

    /**
     * 在 targets 列表中查找匹配目标 IP 的 SNMP 设备配置。
     */
    private SnmpConfig.TargetEntry findDevice(String targetIp) {
        return snmpConfig.getTargets().stream()
                .filter(e -> targetIp.equals(e.getTargetIp()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 执行实际 SNMP 查询，组装 MetricsData。
     */
    private MetricsData doCollect(SnmpConfig.TargetEntry entry, String targetIp) throws IOException {
        CommunityTarget ct = buildCommunityTarget(entry);
        Snmp snmp = null;

        try {
            TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            snmp.listen();

            // 批量查询：接口流量 + TCP 统计
            long now = System.currentTimeMillis();

            long ifInOctets    = getCounter64(snmp, ct, IF_IN_OCTETS + ".1");
            long ifOutOctets   = getCounter64(snmp, ct, IF_OUT_OCTETS + ".1");
            long ifInPkts      = getCounter32(snmp, ct, IF_IN_UCAST_PKTS + ".1");
            long ifOutPkts     = getCounter32(snmp, ct, IF_OUT_UCAST_PKTS + ".1");
            long ifInDiscards  = getCounter32(snmp, ct, IF_IN_DISCARDS + ".1");

            long tcpEstablished = getGauge32(snmp, ct, TCP_CURR_ESTAB);
            long tcpFails       = getCounter32(snmp, ct, TCP_ATTEMPT_FAILS);
            long tcpInSegs      = getCounter32(snmp, ct, TCP_IN_SEGS);
            long tcpOutSegs     = getCounter32(snmp, ct, TCP_OUT_SEGS);

            // 速率计算：字节速率（Bps）、包速率（pps）
            long bytesPerSec = calcRate("ifInOctets." + entry.getDevice(), ifInOctets, now);
            long pktsPerSec  = calcRate("ifInPkts." + entry.getDevice(), ifInPkts, now);
            long tcpFailsPerSec = calcRate("tcpFails." + entry.getDevice(), tcpFails, now);

            // 组装 MetricsData
            MetricsData data = new MetricsData();
            data.setIpAddress(targetIp);
            data.setTimestamp(LocalDateTime.now());

            // 请求速率：用包速率（pps）近似
            data.setRequestRate((int) Math.min(pktsPerSec, Integer.MAX_VALUE));

            // 半开连接数估算：TCP 失败速率 + 当前连接数的比例
            // Slowloris 攻击特征：大量半开连接，established 不高但 fails 高
            int estimatedHalfOpen = (int) Math.min(tcpFailsPerSec * 60 + tcpEstablished / 2, 100_000);
            data.setHalfOpenConns(Math.max(estimatedHalfOpen, 0));

            // 平均包大小：字节速率 / 包速率
            data.setAvgPacketSize(pktsPerSec > 0
                    ? (int) Math.min(bytesPerSec / pktsPerSec, 65535) : 0);

            // 唯一来源 IP 数：SNMP 无法直接获取，设为 0 由上层兜底
            data.setUniqueSourceIps(0);

            // 连接时长：SNMP 不可得，给默认值
            data.setAvgConnDuration(60_000L);

            log.debug("SNMP采集完成 target={} device={} bytes/s={} pkts/s={} tcpEst={} tcpFails/s={} halfOpen≈{}",
                    targetIp, entry.getDevice(), bytesPerSec, pktsPerSec, tcpEstablished, tcpFailsPerSec, estimatedHalfOpen);

            return data;
        } finally {
            if (snmp != null) {
                try { snmp.close(); } catch (Exception ignored) { }
            }
        }
    }

    /**
     * 构建 SNMP v2c CommunityTarget。
     */
    private CommunityTarget buildCommunityTarget(SnmpConfig.TargetEntry entry) {
        CommunityTarget ct = new CommunityTarget();
        ct.setAddress(new UdpAddress(entry.getDevice() + "/" + entry.getPort()));
        ct.setCommunity(new OctetString(snmpConfig.getCommunity()));
        ct.setVersion(SnmpConstants.version2c);
        ct.setTimeout(snmpConfig.getTimeout());
        ct.setRetries(snmpConfig.getRetries());
        return ct;
    }

    // ==================== SNMP GET 辅助方法 ====================

    private long getCounter64(Snmp snmp, CommunityTarget ct, String oid) throws IOException {
        return getSnmpValue(snmp, ct, oid);
    }

    private long getCounter32(Snmp snmp, CommunityTarget ct, String oid) throws IOException {
        return getSnmpValue(snmp, ct, oid);
    }

    private long getGauge32(Snmp snmp, CommunityTarget ct, String oid) throws IOException {
        return getSnmpValue(snmp, ct, oid);
    }

    /**
     * 发起 SNMP GET，返回数值（Counter64/Counter32/Gauge32 通用）。
     */
    private long getSnmpValue(Snmp snmp, CommunityTarget ct, String oid) throws IOException {
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(oid)));
        pdu.setType(PDU.GET);

        ResponseEvent event = snmp.send(pdu, ct);
        if (event == null || event.getResponse() == null) {
            log.debug("SNMP GET {} 无响应", oid);
            return 0L;
        }

        VariableBinding vb = event.getResponse().get(0);
        if (vb == null || vb.getVariable() == null) {
            return 0L;
        }

        Variable var = vb.getVariable();
        if (var instanceof Counter64 c64) {
            return c64.getValue();
        }
        return var.toLong();
    }

    /**
     * 计算速率：本次值 - 上次值 / 时间差（秒）。
     * 首次采集或计数器回绕时返回 0。
     */
    private long calcRate(String key, long currentValue, long now) {
        Long lastVal = lastValues.put(key, currentValue);
        Long lastTime = lastTimes.put(key, now);

        if (lastVal == null || lastTime == null || lastTime >= now) {
            return 0L; // 首次采集，无法计算速率
        }

        long delta = currentValue - lastVal;
        long elapsedSec = (now - lastTime) / 1000;
        if (elapsedSec <= 0) {
            return 0L;
        }

        // 处理计数器回绕（SNMP Counter32 约 4GB 回绕，Counter64 极少回绕）
        if (delta < 0) {
            delta = currentValue; // 近似：视为从 0 重新计数
        }

        return delta / elapsedSec;
    }
}
