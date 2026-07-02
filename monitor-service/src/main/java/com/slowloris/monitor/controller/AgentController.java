package com.slowloris.monitor.controller;

import com.slowloris.common.Result;
import com.slowloris.monitor.entity.MetricsData;
import com.slowloris.monitor.entity.MonitoredTarget;
import com.slowloris.monitor.mapper.MonitoredTargetMapper;
import com.slowloris.monitor.service.impl.AgentReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 主机探针(Agent)接入入口。
 * <ul>
 *   <li>{@code POST /internal/agent/report} —— 探针上报本机真实指标。走 /internal 前缀，
 *       网关不校验 JWT；改用「目标IP + agentToken」匹配鉴权，防止伪造上报。</li>
 *   <li>{@code GET  /internal/agent/install.sh} —— 公开下载安装脚本（curl | bash 一键安装）。</li>
 *   <li>{@code GET  /monitor/agent/install/{ip}} —— 走 JWT，返回该目标拼好的安装命令与探针在线状态，供前端展示。</li>
 * </ul>
 *
 * <p><b>安全提示：</b>上报接口公网可达且不走 JWT，唯一防护是 per-target token。
 * 生产环境必须启用 HTTPS，否则 token 明文传输可被嗅探。</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AgentController {

    private final AgentReportService agentReportService;
    private final MonitoredTargetMapper targetMapper;

    @Value("${agent.report-url:http://localhost:8080}")
    private String reportUrl;

    /**
     * 探针上报。校验「目标IP + token」与 monitored_target 匹配后，缓存指标供采集器读取。
     */
    @PostMapping("/internal/agent/report")
    public Result<Void> report(@RequestBody Map<String, Object> body) {
        String ip = body.get("target") != null ? str(body.get("target")) : str(body.get("ip"));
        String token = str(body.get("token"));
        if (ip == null || ip.isBlank() || token == null || token.isBlank()) {
            return Result.error("缺少 target 或 token");
        }

        MonitoredTarget target = targetMapper.findByIpAndToken(ip, token);
        if (target == null) {
            log.warn("Agent 上报鉴权失败 ip={}", ip);
            return Result.error("鉴权失败");
        }

        agentReportService.record(ip, parseMetrics(ip, body));
        return Result.success();
    }

    /**
     * 公开下载安装脚本。脚本本身是通用的，目标 IP / token / 平台地址由 curl 参数传入。
     */
    @GetMapping(value = "/internal/agent/install.sh")
    public ResponseEntity<String> installScript() throws Exception {
        ClassPathResource res = new ClassPathResource("agent/install.sh");
        String content = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(content);
    }

    /**
     * 返回该目标拼好的一行安装命令 + 探针在线状态，供前端「获取探针安装命令」弹窗使用。
     * 走 /monitor 前缀 → 网关校验 JWT。
     */
    @GetMapping("/monitor/agent/install/{ip}")
    public Result<Map<String, Object>> installCommand(@PathVariable String ip,
                                                      @RequestHeader(value = "user-id", required = false) String userId) {
        MonitoredTarget target = targetMapper.findByIpAndUser(ip, userId);
        if (target == null) {
            return Result.error("该 IP 未在监控中，请先开始监控");
        }
        String token = target.getAgentToken();
        if (token == null || token.isBlank()) {
            return Result.error("该目标缺少探针 token，请停止后重新开始监控以生成");
        }

        String command = "curl -fsSL " + reportUrl + "/internal/agent/install.sh | sudo bash -s -- "
                + "--server=" + reportUrl + " --target=" + ip + " --token=" + token;

        Map<String, Object> data = new HashMap<>();
        data.put("ip", ip);
        data.put("token", token);
        data.put("command", command);
        data.put("online", agentReportService.isOnline(ip));
        data.put("lastSeen", agentReportService.lastSeen(ip));
        return Result.success(data);
    }

    /**
     * 把探针上报的 JSON 字段解析为 MetricsData。
     */
    @SuppressWarnings("unchecked")
    private MetricsData parseMetrics(String ip, Map<String, Object> body) {
        MetricsData data = new MetricsData();
        data.setIpAddress(ip);
        data.setTimestamp(LocalDateTime.now());
        data.setHalfOpenConns((int) toLong(body.get("halfOpenConns")));
        data.setRequestRate((int) toLong(body.get("requestRate")));
        data.setAvgConnDuration(toLong(body.get("avgConnDuration")));
        data.setAvgPacketSize((int) toLong(body.get("avgPacketSize")));
        data.setUniqueSourceIps((int) toLong(body.get("uniqueSourceIps")));

        List<String> sources = new ArrayList<>();
        Object raw = body.get("sourceIps");
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o != null && !o.toString().isBlank()) {
                    sources.add(o.toString());
                }
            }
        }
        data.setSourceIps(sources);
        return data;
    }

    private String str(Object o) {
        return o != null ? o.toString() : null;
    }

    private long toLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        try {
            return o != null ? Long.parseLong(o.toString().trim()) : 0L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
