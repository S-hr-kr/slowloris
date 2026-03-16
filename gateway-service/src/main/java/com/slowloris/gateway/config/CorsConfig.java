package com.slowloris.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Gateway CORS 配置类
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebFilter corsFilter() {
        return (ServerWebExchange ctx, WebFilterChain chain) -> {
            ServerHttpRequest request = ctx.getRequest();
            
            // 如果是预检请求，直接返回
            if (request.getMethod() == HttpMethod.OPTIONS) {
                ServerHttpResponse response = ctx.getResponse();
                response.setStatusCode(HttpStatus.OK);
                return Mono.empty();
            }
            
            ServerHttpResponse response = ctx.getResponse();
            
            // 设置 CORS 头 - 使用 set 而不是 add，避免重复
            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost");
            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "*");
            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
            
            return chain.filter(ctx);
        };
    }
}
