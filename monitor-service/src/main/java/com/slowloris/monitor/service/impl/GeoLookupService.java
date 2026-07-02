package com.slowloris.monitor.service.impl;

import com.alibaba.fastjson2.JSON;
import com.slowloris.monitor.entity.SuspiciousIp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class GeoLookupService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public SuspiciousIp enrich(String ip) {
        String cacheKey = "geo:" + ip;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return JSON.parseObject(JSON.toJSONString(cached), SuspiciousIp.class);
            }
        } catch (Exception e) {
            log.warn("Redis缓存读取失败: {}", e.getMessage());
        }

        try {
            String url = "http://ip-api.com/json/" + ip +
                    "?fields=status,country,countryCode,city,isp&lang=zh-CN";
            Map<?, ?> result = restTemplate.getForObject(url, Map.class);

            SuspiciousIp si = new SuspiciousIp();
            si.setIpAddress(ip);
            if (result != null && "success".equals(result.get("status"))) {
                si.setCountry(toString(result.get("country")));
                si.setCountryCode(toString(result.get("countryCode")));
                si.setCity(toString(result.get("city")));
                si.setIsp(toString(result.get("isp")));
            } else {
                si.setCountry("未知");
                si.setCountryCode("--");
                si.setCity("未知");
                si.setIsp("未知");
            }

            try {
                redisTemplate.opsForValue().set(cacheKey, si, 1, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("Redis缓存写入失败: {}", e.getMessage());
            }
            return si;
        } catch (Exception e) {
            log.warn("IP地理查询失败，IP: {}, 原因: {}", ip, e.getMessage());
            return SuspiciousIp.unknown(ip);
        }
    }

    private String toString(Object o) {
        return o != null ? o.toString() : "未知";
    }
}
