package com.slowloris.auth.controller;

import com.slowloris.auth.service.UserService;
import com.slowloris.auth.vo.LoginVo;
import com.slowloris.auth.vo.RegisterVo;
import com.slowloris.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody RegisterVo registerVo) {
        return userService.register(registerVo);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginVo loginVo) {
        return userService.login(loginVo);
    }

    @GetMapping("/verify")
    public Result<Map<String, Object>> verifyToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        return userService.verifyToken(authHeader);
    }

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("OK");
    }

    @GetMapping("/users")
    public Result<Map<String, Object>> getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) String role) {
        return userService.getUserList(page, limit, role);
    }

    @PostMapping("/users/create")
    public Result<Map<String, Object>> createUser(@RequestBody Map<String, Object> params) {
        return userService.createUser(params);
    }

    @PutMapping("/users/update/{id}")
    public Result<Map<String, Object>> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        Map<String, Object> sanitizedParams = new HashMap<>();
        String[] allowedFields = {"email", "role", "status", "password"};
        for (String field : allowedFields) {
            if (params.containsKey(field)) {
                Object value = params.get(field);
                if (value instanceof String) {
                    sanitizedParams.put(field, escapeHtml((String) value));
                } else {
                    sanitizedParams.put(field, value);
                }
            }
        }
        return userService.updateUser(id, sanitizedParams);
    }

    @DeleteMapping("/users/delete/{id}")
    public Result<String> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id);
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

}
