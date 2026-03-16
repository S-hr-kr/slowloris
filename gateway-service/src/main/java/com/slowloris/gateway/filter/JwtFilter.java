package com.slowloris.gateway.filter;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class JwtFilter extends AbstractGatewayFilterFactory<JwtFilter.Config> {

    @Value("${jwt.secret}")
    private String jwtSecret;

    public JwtFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // Skip authentication for OPTIONS requests
            if (request.getMethod().name().equals("OPTIONS")) {
                return chain.filter(exchange);
            }

            // Skip authentication for login, register, verify and health endpoints
            if (path.startsWith("/login") ||
                path.startsWith("/register") ||
                path.startsWith("/verify") ||
                path.startsWith("/health") ||
                path.startsWith("/auth/verify") ||
                path.startsWith("/auth/health") ||
                path.startsWith("/ws/")) {
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "No authorization header found", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // Add user info to headers
                request = request.mutate()
                        .header("user-id", claims.get("userId", String.class))
                        .header("username", claims.get("username", String.class))
                        .header("token", token)
                        .header("roles", String.join(",", claims.get("roles", List.class)))
                        .build();

                exchange = exchange.mutate().request(request).build();

            } catch (SignatureException | MalformedJwtException ex) {
                return onError(exchange, "Invalid JWT signature", HttpStatus.UNAUTHORIZED);
            } catch (ExpiredJwtException ex) {
                return onError(exchange, "Expired JWT token", HttpStatus.UNAUTHORIZED);
            } catch (UnsupportedJwtException ex) {
                return onError(exchange, "Unsupported JWT token", HttpStatus.UNAUTHORIZED);
            } catch (IllegalArgumentException ex) {
                return onError(exchange, "JWT claims string is empty", HttpStatus.UNAUTHORIZED);
            }

            return chain.filter(exchange);
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        log.error("JWT Filter Error: {}", err);
        return response.setComplete();
    }

    public static class Config {
        // Configuration properties if needed
    }

}