package com.slowloris.auth.controller;

import com.slowloris.auth.service.UserService;
import com.slowloris.auth.vo.LoginVo;
import com.slowloris.auth.vo.RegisterVo;
import com.slowloris.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterVo registerVo) {
        return userService.register(registerVo);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginVo loginVo) {
        return userService.login(loginVo);
    }

    @GetMapping("/verify")
    public Result<Map<String, Object>> verifyToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return userService.verifyToken(authHeader);
    }

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("OK");
    }

    // GET /api/users
    @GetMapping("/users")
    public Result<List<Map<String, Object>>> getUserList() {
        return userService.getUserList();
    }

    // POST /api/users
    @PostMapping("/users")
    public Result<String> createUser(@RequestBody Map<String, Object> params) {
        Map<String, Object> sanitized = sanitize(params, "username", "email", "role", "status");
        sanitized.put("password", params.get("password")); // 密码不转义，由 service 层 BCrypt 处理
        return userService.createUser(sanitized);
    }

    // PUT /api/users/{id}
    @PutMapping("/users/{id}")
    public Result<String> updateUser(@PathVariable Long id,
                                     @RequestBody Map<String, Object> params) {
        Map<String, Object> sanitized = sanitize(params, "email", "role", "status");
        if (params.containsKey("password")) {
            sanitized.put("password", params.get("password"));
        }
        return userService.updateUser(id, sanitized);
    }

    // DELETE /api/users/{id}
    @DeleteMapping("/users/{id}")
    public Result<String> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id);
    }

    // POST /api/reset-password
    @PostMapping("/reset-password")
    public Result<String> resetPassword(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String newPassword = (String) body.get("newPassword");
        if (username == null || username.isBlank()) return Result.error("用户名不能为空");
        if (newPassword == null || newPassword.length() < 6) return Result.error("密码至少6位");
        return userService.resetPassword(escapeHtml(username), newPassword);
    }

    // GET /api/profile —— 当前登录用户的个人资料
    @GetMapping("/profile")
    public Result<Map<String, Object>> getProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return userService.getProfile(authHeader);
    }

    // PUT /api/profile —— 更新当前登录用户资料（用户名/邮箱/密码/头像）
    @PutMapping("/profile")
    public Result<Map<String, Object>> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> params) {
        Map<String, Object> sanitized = new HashMap<>();
        // 文本字段做 HTML 转义；头像与密码原样透传（由 service 校验/加密）
        for (String f : new String[]{"username", "email"}) {
            if (params.containsKey(f)) {
                Object v = params.get(f);
                sanitized.put(f, v instanceof String ? escapeHtml((String) v) : v);
            }
        }
        for (String f : new String[]{"avatar", "oldPassword", "newPassword"}) {
            if (params.containsKey(f)) sanitized.put(f, params.get(f));
        }
        return userService.updateProfile(authHeader, sanitized);
    }

    private Map<String, Object> sanitize(Map<String, Object> params, String... fields) {
        Map<String, Object> result = new HashMap<>();
        for (String field : fields) {
            if (params.containsKey(field)) {
                Object val = params.get(field);
                result.put(field, val instanceof String ? escapeHtml((String) val) : val);
            }
        }
        return result;
    }

    private String escapeHtml(String input) {
        if (input == null) return null;
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
