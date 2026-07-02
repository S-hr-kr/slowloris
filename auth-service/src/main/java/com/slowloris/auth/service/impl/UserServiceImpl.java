package com.slowloris.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
    private static final String REDIS_USER_PREFIX = "user:";
    private static final String REDIS_REFRESH_PREFIX = "refresh_token:";
    private static final String REDIS_USER_LIST = "user:list:all";
    private static final int STATUS_ACTIVE = 1;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_DATE_TIME;

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

    @Override
    public Result<String> register(RegisterVo registerVo) {
        if (getUserByUsername(registerVo.getUsername()) != null) {
            return Result.error("用户名已存在");
        }
        User user = new User();
        user.setUsername(registerVo.getUsername());
        user.setPassword(passwordEncoder.encode(registerVo.getPassword()));
        user.setRoles(registerVo.getRole() != null ? registerVo.getRole().toUpperCase() : "USER");
        user.setStatus(STATUS_ACTIVE);
        user.setEmail(registerVo.getEmail());
        save(user);
        redisTemplate.delete(REDIS_USER_LIST);
        return Result.success("注册成功");
    }

    @Override
    public Result<Map<String, Object>> login(LoginVo loginVo) {
        User user = getUserByUsername(loginVo.getUsername());
        if (user == null || !passwordEncoder.matches(loginVo.getPassword(), user.getPassword())) {
            return Result.error("用户名或密码错误");
        }
        if (user.getStatus() != STATUS_ACTIVE) {
            return Result.error("账户已禁用");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("username", user.getUsername());
        claims.put("roles", Arrays.asList(user.getRoles().split(",")));

        String accessToken = generateToken(claims, jwtExpiration);
        String refreshToken = generateToken(claims, jwtRefreshExpiration);

        redisTemplate.opsForValue().set(
                REDIS_REFRESH_PREFIX + user.getId(),
                refreshToken,
                jwtRefreshExpiration,
                //过期时间30minutes
                TimeUnit.HOURS
        );

        Map<String, Object> data = new HashMap<>();
        data.put("token", accessToken);
        return Result.success(data, "登录成功");
    }

    @Override
    public User getUserByUsername(String username) {
        String cacheKey = REDIS_USER_PREFIX + username;
        User cached = (User) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return cached;
        User user = userMapper.selectByUsername(username);
        if (user != null) {
            redisTemplate.opsForValue().set(cacheKey, user, 30L, TimeUnit.MINUTES);
        }
        return user;
    }

    @Override
    public TokenVo refreshToken(String refreshToken) {
        Claims claims = parseToken(refreshToken);
        String userId = claims.get("userId", String.class);
        String stored = (String) redisTemplate.opsForValue().get(REDIS_REFRESH_PREFIX + userId);
        if (stored == null || !stored.equals(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }
        String newToken = generateToken(claims, jwtExpiration);
        TokenVo vo = new TokenVo();
        vo.setAccessToken(newToken);
        vo.setRefreshToken(refreshToken);
        vo.setExpiresIn(jwtExpiration / 1000);
        return vo;
    }

    @Override
    public Result<Map<String, Object>> verifyToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return Result.error("无效的认证令牌");
        }
        try {
            Claims claims = parseToken(authHeader.substring(BEARER_PREFIX.length()));
            User user = getUserByUsername(claims.get("username", String.class));
            if (user == null) return Result.error("用户不存在");
            Map<String, Object> data = new HashMap<>();
            data.put("valid", true);
            data.put("user", toBasicMap(user));
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("令牌无效或过期");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result<List<Map<String, Object>>> getUserList() {
        List<Map<String, Object>> cached =
                (List<Map<String, Object>>) redisTemplate.opsForValue().get(REDIS_USER_LIST);
        if (cached != null) return Result.success(cached);

        List<User> users = list(new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime));
        List<Map<String, Object>> list = users.stream().map(this::toDetailMap).collect(Collectors.toList());
        redisTemplate.opsForValue().set(REDIS_USER_LIST, list, 2L, TimeUnit.MINUTES);
        return Result.success(list);
    }

    @Override
    public Result<String> createUser(Map<String, Object> params) {
        String username = (String) params.get("username");
        String password = (String) params.get("password");
        String email    = (String) params.get("email");
        String role     = (String) params.get("role");

        if (username == null || password == null || email == null || role == null) {
            return Result.error("请求参数不完整");
        }
        if (getUserByUsername(username) != null) {
            return Result.error("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRoles(role.toUpperCase());
        user.setStatus(STATUS_ACTIVE);
        save(user);
        redisTemplate.delete(REDIS_USER_LIST);
        return Result.success("用户创建成功");
    }

    @Override
    public Result<String> updateUser(Long id, Map<String, Object> params) {
        User user = getById(id);
        if (user == null) return Result.error("用户不存在");

        Optional.ofNullable((String) params.get("email")).ifPresent(user::setEmail);
        Optional.ofNullable((String) params.get("role")).ifPresent(r -> user.setRoles(r.toUpperCase()));
        Optional.ofNullable((String) params.get("status")).ifPresent(s ->
                user.setStatus("active".equals(s) ? STATUS_ACTIVE : 0));
        Optional.ofNullable((String) params.get("password")).ifPresent(p ->
                user.setPassword(passwordEncoder.encode(p)));

        updateById(user);
        redisTemplate.delete(REDIS_USER_PREFIX + user.getUsername());
        redisTemplate.delete(REDIS_USER_LIST);
        return Result.success("用户更新成功");
    }

    @Override
    public Result<String> deleteUser(Long id) {
        User user = getById(id);
        if (user == null) return Result.error("用户不存在");
        removeById(id);
        redisTemplate.delete(REDIS_USER_PREFIX + user.getUsername());
        redisTemplate.delete(REDIS_USER_LIST);
        return Result.success("用户删除成功");
    }

    @Override
    public Result<String> resetPassword(String username, String newPassword) {
        User user = getUserByUsername(username);
        if (user == null) return Result.error("用户不存在");
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
        redisTemplate.delete(REDIS_USER_PREFIX + username);
        return Result.success("密码重置成功");
    }

    @Override
    public Result<Map<String, Object>> getProfile(String authHeader) {
        User user = currentUser(authHeader);
        if (user == null) return Result.error("令牌无效或用户不存在");
        return Result.success(toProfileMap(user));
    }

    @Override
    public Result<Map<String, Object>> updateProfile(String authHeader, Map<String, Object> params) {
        User user = currentUser(authHeader);
        if (user == null) return Result.error("令牌无效或用户不存在");

        String oldUsername = user.getUsername();
        boolean credentialChanged = false;

        // ── 用户名 ──
        String newUsername = trimToNull((String) params.get("username"));
        if (newUsername != null && !newUsername.equals(user.getUsername())) {
            if (newUsername.length() < 3 || newUsername.length() > 64) {
                return Result.error("用户名长度需在 3-64 个字符之间");
            }
            User exists = userMapper.selectByUsername(newUsername);
            if (exists != null && !exists.getId().equals(user.getId())) {
                return Result.error("用户名已被占用");
            }
            user.setUsername(newUsername);
            credentialChanged = true;
        }

        // ── 邮箱 ──
        if (params.containsKey("email")) {
            String email = trimToNull((String) params.get("email"));
            if (email != null && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                return Result.error("邮箱格式不正确");
            }
            user.setEmail(email);
        }

        // ── 头像（Base64 Data URL，前端已压缩） ──
        if (params.containsKey("avatar")) {
            String avatar = (String) params.get("avatar");
            if (avatar != null && !avatar.isBlank()) {
                if (!avatar.startsWith("data:image/")) {
                    return Result.error("头像格式不正确");
                }
                if (avatar.length() > 1_400_000) { // ~1MB 原始图片的 Base64 上限
                    return Result.error("头像文件过大，请选择更小的图片");
                }
            }
            user.setAvatar(avatar);
        }

        // ── 密码（需校验原密码） ──
        String newPassword = (String) params.get("newPassword");
        if (newPassword != null && !newPassword.isEmpty()) {
            String oldPassword = (String) params.get("oldPassword");
            if (oldPassword == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
                return Result.error("原密码错误");
            }
            if (newPassword.length() < 6) {
                return Result.error("新密码至少6位");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            credentialChanged = true;
        }

        updateById(user);
        // 清理旧用户名与新用户名缓存，避免脏数据
        redisTemplate.delete(REDIS_USER_PREFIX + oldUsername);
        redisTemplate.delete(REDIS_USER_PREFIX + user.getUsername());
        redisTemplate.delete(REDIS_USER_LIST);

        Map<String, Object> data = toProfileMap(user);
        // 用户名或密码变更会使现有 JWT 失效，提示前端重新登录
        data.put("requireRelogin", credentialChanged);
        return Result.success(data, credentialChanged ? "资料已更新，请重新登录" : "资料已更新");
    }

    // ---- 私有工具方法 ----

    /** 从 Authorization 头解析并加载当前登录用户 */
    private User currentUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) return null;
        try {
            Claims claims = parseToken(authHeader.substring(BEARER_PREFIX.length()));
            String username = claims.get("username", String.class);
            if (username == null) return null;
            // 直接走 DB，避免命中可能过期的用户名缓存
            return userMapper.selectByUsername(username);
        } catch (Exception e) {
            return null;
        }
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private Map<String, Object> toProfileMap(User user) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("email", user.getEmail());
        m.put("avatar", user.getAvatar());
        m.put("role", user.getRoles() != null ? user.getRoles().toLowerCase() : "user");
        return m;
    }

    private String generateToken(Map<String, Object> claims, Long expiration) {
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .compact();
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Map<String, Object> toBasicMap(User user) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("role", user.getRoles().toLowerCase());
        m.put("email", user.getEmail());
        m.put("avatar", user.getAvatar());
        return m;
    }

    private Map<String, Object> toDetailMap(User user) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("email", user.getEmail());
        m.put("role", user.getRoles() != null ? user.getRoles().toLowerCase() : "user");
        m.put("status", user.getStatus() == STATUS_ACTIVE ? "active" : "inactive");
        m.put("avatar", user.getAvatar());
        m.put("createdAt", user.getCreateTime() != null ? user.getCreateTime().format(FMT) : null);
        m.put("lastLogin", user.getUpdateTime() != null ? user.getUpdateTime().format(FMT) : null);
        return m;
    }
}
