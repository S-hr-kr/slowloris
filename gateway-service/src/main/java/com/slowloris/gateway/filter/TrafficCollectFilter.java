package com.slowloris.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.InetSocketAddress;
import java.util.Map;

@Slf4j
@Component
public class TrafficCollectFilter implements GlobalFilter, Ordered {

    private final WebClient webClient;

    @Value("${monitor.service.url:http://localhost:8082}")
    private String monitorServiceUrl;

    public TrafficCollectFilter(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 跳过内部上报接口本身，避免递归
        if (path.contains("/traffic/record") || path.startsWith("/ws")) {
            return chain.filter(exchange);
        }

        String ip = resolveClientIp(request);
        if (ip == null || ip.isBlank()) return chain.filter(exchange);

        // 异步上报，不阻塞主链路
        webClient.post()
                .uri(monitorServiceUrl + "/traffic/record")
                .bodyValue(Map.of("ip", ip, "size", 0))
                .retrieve()
                .bodyToMono(Void.class)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(null, e -> log.debug("traffic record failed: {}", e.getMessage()));

        return chain.filter(exchange);
    }

    private String resolveClientIp(ServerHttpRequest request) {
        String xff = request.getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeaders().getFirst("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri.trim();
        InetSocketAddress addr = request.getRemoteAddress();
        return addr != null ? addr.getAddress().getHostAddress() : null;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
