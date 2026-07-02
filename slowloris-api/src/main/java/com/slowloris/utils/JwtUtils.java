package com.slowloris.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.*;

/**
 * JWT工具类 - 提供Token生成、解析、验证等通用方法
 * 供各微服务模块共享使用
 */
public class JwtUtils {

    /**
     * 生成JWT Token
     *
     * @param claims     自定义声明
     * @param secret     密钥
     * @param expiration 过期时间（毫秒）
     * @return Token字符串
     */
    public static String generateToken(Map<String, Object> claims, String secret, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 解析JWT Token
     *
     * @param token  Token字符串
     * @param secret 密钥
     * @return Claims声明
     * @throws JwtException Token无效或过期时抛出异常
     */
    public static Claims parseToken(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证Token是否有效
     *
     * @param token  Token字符串
     * @param secret 密钥
     * @return true-有效，false-无效或过期
     */
    public static boolean validateToken(String token, String secret) {
        try {
            parseToken(token, secret);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 判断Token是否已过期
     *
     * @param token  Token字符串
     * @param secret 密钥
     * @return true-已过期
     */
    public static boolean isTokenExpired(String token, String secret) {
        try {
            Claims claims = parseToken(token, secret);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 从Token中获取用户名
     *
     * @param token  Token字符串
     * @param secret 密钥
     * @return 用户名
     */
    public static String getUsername(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return claims.get("username", String.class);
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token  Token字符串
     * @param secret 密钥
     * @return 用户ID
     */
    public static String getUserId(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return claims.get("userId", String.class);
    }

    /**
     * 从Token中获取角色列表
     *
     * @param token  Token字符串
     * @param secret 密钥
     * @return 角色列表
     */
    @SuppressWarnings("unchecked")
    public static List<String> getRoles(String token, String secret) {
        Claims claims = parseToken(token, secret);
        Object roles = claims.get("roles");
        if (roles instanceof List) {
            return (List<String>) roles;
        }
        if (roles instanceof String) {
            return Arrays.asList(((String) roles).split(","));
        }
        return Collections.emptyList();
    }

    /**
     * 从Authorization头中提取Token
     *
     * @param authHeader Authorization头（格式: "Bearer xxx"）
     * @return Token字符串，如果格式不正确返回null
     */
    public static String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 获取Token的剩余有效时间（毫秒）
     *
     * @param token  Token字符串
     * @param secret 密钥
     * @return 剩余有效时间（毫秒），已过期返回0
     */
    public static long getRemainingTime(String token, String secret) {
        try {
            Claims claims = parseToken(token, secret);
            Date expiration = claims.getExpiration();
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(remaining, 0);
        } catch (Exception e) {
            return 0;
        }
    }
}
