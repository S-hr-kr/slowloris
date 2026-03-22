package com.slowloris.monitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * REST 客户端配置类（统一管理 RestTemplate + 远程服务地址）
 */
@Configuration // 标记为Spring配置类，使Spring扫描并加载
public class RestConfig {

    // ========== 可配置化远程服务地址（替代硬编码，支持配置文件覆盖） ==========
    @Value("${monitor.rest.url.prefix:http://localhost:8080}") // 从配置文件读取，默认值兜底
    private String restUrlPrefix;

    // ========== Prediction Service 远程地址（攻击预测服务） ==========
    @Value("${prediction.service.url:${monitor.rest.url.prefix}/api/v1/prediction}")
    private String predictionServiceUrl;

    // ========== IP 地域查询接口地址（可选） ==========
    @Value("${ip.region.service.url:${monitor.rest.url.prefix}/api/v1/ip/region}")
    private String ipRegionServiceUrl;

    // ========== 核心：配置 RestTemplate Bean（全局可用） ==========
    @Bean
    public RestTemplate restTemplate() {
        // 1. 配置请求工厂（设置超时，避免请求阻塞）
        ClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        ((SimpleClientHttpRequestFactory) factory).setConnectTimeout(5000); // 连接超时5秒
        ((SimpleClientHttpRequestFactory) factory).setReadTimeout(10000);    // 读取超时10秒

        // 2. 创建RestTemplate并设置工厂
        RestTemplate restTemplate = new RestTemplate(factory);

        // 3. 可选：添加消息转换器（支持JSON/Form表单）
        // restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        return restTemplate;
    }

    // ========== 对外提供常量/方法（方便业务类调用） ==========
    // 原有的静态常量（兼容旧代码）
    public static final String DEFAULT_REST_URL_PREFIX = "http://localhost:8080";

    // 获取远程服务前缀（推荐使用实例方法，支持配置动态修改）
    public String getRestUrlPrefix() {
        return restUrlPrefix;
    }

    // 获取攻击预测服务完整地址
    public String getPredictionServiceUrl() {
        return predictionServiceUrl;
    }

    // 获取IP地域查询服务地址
    public String getIpRegionServiceUrl() {
        return ipRegionServiceUrl;
    }
}