package com.slowloris.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slowloris.auth.entity.User;
import com.slowloris.auth.mapper.UserMapper;
import com.slowloris.auth.service.UserService;
import com.slowloris.auth.vo.LoginVo;
import com.slowloris.auth.vo.RegisterVo;
import com.slowloris.auth.vo.TokenVo;
import com.slowloris.common.Result;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String REDIS_KEY_USER_PREFIX = "user:";
    private static final String REDIS_KEY_REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String DEFAULT_ROLE = "USER";
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_DISABLED = 0;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String jwtSecret;
    private final Long jwtExpiration;
    private final Long jwtRefreshExpiration;

    public UserServiceImpl(UserMapper userMapper,
                           RedisTemplate<String, Object> redisTemplate,
                           BCryptPasswordEncoder passwordEncoder,
                           @Value("${jwt.secret}") String jwtSecret,
                           @Value("${jwt.expiration}") Long jwtExpiration,
                           @Value("${jwt.refresh-expiration}") Long jwtRefreshExpiration) {
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
        this.jwtSecret = jwtSecret;
        this.jwtExpiration = jwtExpiration;
        this.jwtRefreshExpiration = jwtRefreshExpiration;
    }

    // ==================== 业务方法实现 ====================

    @Override
    public Result<Map<String, Object>> register(RegisterVo registerVo) {
        // 1. 检查用户名
        if (getUserByUsername(registerVo.getUsername()) != null) {
            return Result.error("用户名已存在");
        }

        // 2. 构建用户实体
        User user = new User();
        user.setUsername(registerVo.getUsername());
        user.setPassword(passwordEncoder.encode(registerVo.getPassword()));
        user.setRoles(registerVo.getRole() != null ? registerVo.getRole().toUpperCase() : DEFAULT_ROLE);
        user.setStatus(STATUS_ACTIVE);
        user.setEmail(registerVo.getEmail());
        save(user);

        // 3. 返回数据
        User savedUser = getUserByUsername(registerVo.getUsername());
        return Result.success(convertToBasicMap(savedUser), "注册成功");
    }

    @Override
    public Result<Map<String, Object>> login(LoginVo loginVo) {
        // 1. 校验用户
        User user = getUserByUsername(loginVo.getUsername());
        if (user == null || !passwordEncoder.matches(loginVo.getPassword(), user.getPassword())) {
            return Result.error("用户名或密码错误");
        }
        if (user.getStatus() == STATUS_DISABLED) {
            return Result.error("账户已禁用");
        }

        // 2. 生成 Token
        Map<String, Object> claims = buildClaims(user);
        String accessToken = generateToken(claims, jwtExpiration);
        String refreshToken = generateToken(claims, jwtRefreshExpiration);

        // 3. 存储 Refresh Token
        redisTemplate.opsForValue().set(
                REDIS_KEY_REFRESH_TOKEN_PREFIX + user.getId(),
                refreshToken,
                jwtRefreshExpiration,
                TimeUnit.MILLISECONDS
        );

        // 4. 组装返回
        Map<String, Object> data = new HashMap<>();
        data.put("token", accessToken);
        data.put("user", convertToBasicMap(user));
        return Result.success(data, "登录成功");
    }

    @Override
    public User getUserByUsername(String username) {
        // 1. 查缓存
        String cacheKey = REDIS_KEY_USER_PREFIX + username;
        User cachedUser = (User) redisTemplate.opsForValue().get(cacheKey);
        if (cachedUser != null) {
            return cachedUser;
        }

        // 2. 查数据库
        User dbUser = userMapper.selectByUsername(username);
        if (dbUser != null) {
            redisTemplate.opsForValue().set(cacheKey, dbUser, 30L, TimeUnit.MINUTES);
        }
        return dbUser;
    }

    @Override
    public TokenVo refreshToken(String refreshToken) {
        // 1. 解析 Token
        Claims claims = parseToken(refreshToken);
        String userId = claims.get("userId", String.class);

        // 2. 校验 Redis 中的 Refresh Token
        String storedToken = (String) redisTemplate.opsForValue().get(REDIS_KEY_REFRESH_TOKEN_PREFIX + userId);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        // 3. 生成新 Access Token
        String newAccessToken = generateToken(claims, jwtExpiration);

        TokenVo tokenVo = new TokenVo();
        tokenVo.setAccessToken(newAccessToken);
        tokenVo.setRefreshToken(refreshToken); // 这里可以选择生成新的 refresh_token 也可以复用
        tokenVo.setExpiresIn(jwtExpiration / 1000);
        return tokenVo;
    }

    @Override
    public Result<Map<String, Object>> verifyToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return Result.error("无效的认证令牌");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            Claims claims = parseToken(token);
            String username = claims.get("username", String.class);
            User user = getUserByUsername(username);

            if (user == null) {
                return Result.error("用户不存在");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("valid", true);
            data.put("user", convertToBasicMap(user));
            return Result.success(data);
        } catch (Exception e) {
            log.error("Token verification failed: {}", e.getMessage());
            return Result.error("令牌无效或过期");
        }
    }

    @Override
    public Result<Map<String, Object>> getUserList(Integer page, Integer limit, String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (role != null) {
            wrapper.like(User::getRoles, role.toUpperCase());
        }
        wrapper.orderByDesc(User::getCreateTime);

        Page<User> pageResult = page(new Page<>(page, limit), wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("total", pageResult.getTotal());
        // 使用 Stream 简化转换
        List<Map<String, Object>> list = pageResult.getRecords().stream()
                .map(this::convertToDetailMap)
                .collect(Collectors.toList());
        data.put("list", list);

        return Result.success(data);
    }

    @Override
    public Result<Map<String, Object>> createUser(Map<String, Object> params) {
        String username = (String) params.get("username");
        String password = (String) params.get("password");
        String email = (String) params.get("email");
        String role = (String) params.get("role");
        String status = (String) params.get("status");

        if (username == null || password == null || email == null || role == null) {
            return Result.error("请求参数错误");
        }
        if (getUserByUsername(username) != null) {
            return Result.error("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRoles(role.toUpperCase());
        user.setStatus("active".equals(status) ? STATUS_ACTIVE : STATUS_DISABLED);
        save(user);

        // 重新查询以确保获取到 createTime 等数据库自动生成的字段
        return Result.success(convertToDetailMap(getById(user.getId())), "用户创建成功");
    }

    @Override
    public Result<Map<String, Object>> updateUser(Long id, Map<String, Object> params) {
        User user = getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 简化参数赋值
        Optional.ofNullable((String) params.get("email")).ifPresent(user::setEmail);
        Optional.ofNullable((String) params.get("role")).ifPresent(r -> user.setRoles(r.toUpperCase()));
        Optional.ofNullable((String) params.get("status")).ifPresent(s -> user.setStatus("active".equals(s) ? STATUS_ACTIVE : STATUS_DISABLED));
        Optional.ofNullable((String) params.get("password")).ifPresent(p -> user.setPassword(passwordEncoder.encode(p)));

        updateById(user);

        Map<String, Object> data = convertToDetailMap(user);
        data.put("updated_at", LocalDateTime.now().format(FORMATTER));
        data.remove("created_at"); // 更新接口通常不需要返回创建时间
        return Result.success(data, "用户更新成功");
    }

    @Override
    public Result<String> deleteUser(Long id) {
        if (getById(id) == null) {
            return Result.error("用户不存在");
        }
        removeById(id);
        return Result.success("用户删除成功");
    }


    /**
     * 构建 JWT Claims
     */
    private Map<String, Object> buildClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("username", user.getUsername());
        claims.put("roles", Arrays.asList(user.getRoles().split(",")));
        return claims;
    }

    /**
     * 生成 Token
     * 使用新版本 JJWT API
     */
    private String generateToken(Map<String, Object> claims, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .compact();
    }

    /**
     * 解析 Token
     */
    private Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new RuntimeException("Refresh token expired");
        } catch (Exception ex) {
            throw new RuntimeException("Invalid refresh token");
        }
    }

    /**
     * 用户实体转基础 Map (用于 Login/Register/Basic Info)
     */
    private Map<String, Object> convertToBasicMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("role", user.getRoles().toLowerCase());
        map.put("email", user.getEmail());
        return map;
    }

    /**
     * 用户实体转详细 Map (用于 List/Detail)
     */
    private Map<String, Object> convertToDetailMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("role", user.getRoles().toLowerCase());
        map.put("status", user.getStatus() == STATUS_ACTIVE ? "active" : "disabled");
        map.put("created_at", user.getCreateTime() != null ? user.getCreateTime().format(FORMATTER) : null);
        return map;
    }
}