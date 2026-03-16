package com.slowloris.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

/**
 * WebSocket 配置类 - 支持原生 WebSocket 和 SockJS 降级传输
 */
@Configuration
public class WebSocketConfig {

    /**
     * SockJS info endpoint - 支持 /ws/info 和 /api/ws/info
     */
    @Bean
    public RouterFunction<ServerResponse> sockJsInfoRouter() {
        return route(GET("/ws/info"), request -> {
            Map<String, Object> info = new HashMap<>();
            info.put("websocket", true);
            info.put("origins", new String[]{"*:*"});
            info.put("cookie_needed", false);
            info.put("entropy", Math.random());
            return ok().contentType(MediaType.APPLICATION_JSON).bodyValue(info);
        }).and(route(GET("/api/ws/info"), request -> {
            Map<String, Object> info = new HashMap<>();
            info.put("websocket", true);
            info.put("origins", new String[]{"*:*"});
            info.put("cookie_needed", false);
            info.put("entropy", Math.random());
            return ok().contentType(MediaType.APPLICATION_JSON).bodyValue(info);
        }));
    }

    /**
     * SockJS XHR 流式传输端点 - 支持 /ws/{server}/{session}/xhr_streaming
     */
    @Bean
    public RouterFunction<ServerResponse> sockJsXhrStreamingRouter() {
        // POST /ws/{server}/{session}/xhr_streaming
        return route(POST("/ws/{serverId}/{sessionId}/xhr_streaming"), request -> {
            String sessionId = request.pathVariable("sessionId");
            // 发送 SockJS 打开帧
            String openFrame = "o";
            // 模拟心跳保持连接
            Flux<String> stream = Flux.interval(Duration.ofSeconds(30))
                    .map(i -> "h") // 心跳帧
                    .startWith(openFrame);
            return ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(stream.map(data -> data + "\n"), String.class);
        }).and(route(POST("/api/ws/{serverId}/{sessionId}/xhr_streaming"), request -> {
            String sessionId = request.pathVariable("sessionId");
            String openFrame = "o";
            Flux<String> stream = Flux.interval(Duration.ofSeconds(30))
                    .map(i -> "h")
                    .startWith(openFrame);
            return ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(stream.map(data -> data + "\n"), String.class);
        }));
    }

    /**
     * SockJS XHR 轮询端点 - 支持 /ws/{server}/{session}/xhr
     */
    @Bean
    public RouterFunction<ServerResponse> sockJsXhrPollingRouter() {
        // POST /ws/{server}/{session}/xhr
        return route(POST("/ws/{serverId}/{sessionId}/xhr"), request -> {
            String sessionId = request.pathVariable("sessionId");
            // 发送 SockJS 打开帧和欢迎消息
            String response = "o\na[\"Hello from SockJS\"]";
            return ok()
                    .contentType(MediaType.parseMediaType("text/plain; charset=UTF-8"))
                    .bodyValue(response);
        }).and(route(POST("/api/ws/{serverId}/{sessionId}/xhr"), request -> {
            String sessionId = request.pathVariable("sessionId");
            String response = "o\na[\"Hello from SockJS\"]";
            return ok()
                    .contentType(MediaType.parseMediaType("text/plain; charset=UTF-8"))
                    .bodyValue(response);
        }));
    }

    /**
     * SockJS EventSource (SSE) 端点 - 支持 /ws/{server}/{session}/eventsource
     */
    @Bean
    public RouterFunction<ServerResponse> sockJsEventSourceRouter() {
        // GET /ws/{server}/{session}/eventsource
        return route(GET("/ws/{serverId}/{sessionId}/eventsource"), request -> {
            String sessionId = request.pathVariable("sessionId");
            // SSE 格式的数据
            Flux<String> eventStream = Flux.interval(Duration.ofSeconds(30))
                    .map(i -> "data: h\n\n"); // 心跳
            return ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(eventStream, String.class);
        }).and(route(GET("/api/ws/{serverId}/{sessionId}/eventsource"), request -> {
            String sessionId = request.pathVariable("sessionId");
            Flux<String> eventStream = Flux.interval(Duration.ofSeconds(30))
                    .map(i -> "data: h\n\n");
            return ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(eventStream, String.class);
        }));
    }

    /**
     * SockJS iframe.html - 用于旧浏览器兼容
     */
    @Bean
    public RouterFunction<ServerResponse> sockJsIframeRouter() {
        return route(GET("/ws/iframe.html"), request -> {
            String html = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body><h1>SockJS iframe</h1></body></html>";
            return ok()
                    .contentType(MediaType.TEXT_HTML)
                    .bodyValue(html);
        }).and(route(GET("/api/ws/iframe.html"), request -> {
            String html = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body><h1>SockJS iframe</h1></body></html>";
            return ok()
                    .contentType(MediaType.TEXT_HTML)
                    .bodyValue(html);
        }));
    }

    /**
     * WebSocket 处理器
     */
    @Bean
    public HandlerMapping webSocketMapping() {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        
        Map<String, WebSocketHandler> map = new HashMap<>();
        // 支持多种 WebSocket 端点路径
        // 原生 WebSocket
        map.put("/ws/**", webSocketHandler());
        map.put("/api/ws/**", webSocketHandler());
        // SockJS 降级传输端点 (XHR Streaming, XHR Polling, SSE 等)
        map.put("/sockjs/**", webSocketHandler());
        map.put("/api/sockjs/**", webSocketHandler());
        
        mapping.setUrlMap(map);
        mapping.setOrder(10);
        
        return mapping;
    }

    /**
     * WebSocket 处理器实现
     */
    @Bean
    public WebSocketHandler webSocketHandler() {
        return session -> {
            // 接收消息
            Mono<Void> receive = session.receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .doOnNext(message -> System.out.println("Received: " + message))
                    .then();

            // 发送消息
            Mono<Void> send = session.send(
                    Mono.just(session.textMessage("Hello from WebSocket"))
            );

            // 同时执行接收和发送，使用 thenMany 替代 zip 避免阻塞
            return receive.then(send);
        };
    }
}