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

@Configuration
public class CorsConfig {

    @Bean
    public WebFilter corsFilter() {
        return (ServerWebExchange ctx, WebFilterChain chain) -> {
            ServerHttpRequest request = ctx.getRequest();
            ServerHttpResponse response = ctx.getResponse();

            String origin = request.getHeaders().getFirst(HttpHeaders.ORIGIN);

            // 先清除下游服务可能已设置的 CORS 头，避免重复
            response.getHeaders().remove(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
            response.getHeaders().remove(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
            response.getHeaders().remove(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS);
            response.getHeaders().remove(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);

            if (origin != null) {
                response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            } else {
                response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            }
            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS, PATCH");
            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "*");
            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");

            if (request.getMethod() == HttpMethod.OPTIONS) {
                response.setStatusCode(HttpStatus.OK);
                return Mono.empty();
            }

            return chain.filter(ctx);
        };
    }
}
