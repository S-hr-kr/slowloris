package com.slowloris.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.slowloris.auth.entity.User;
import com.slowloris.auth.vo.LoginVo;
import com.slowloris.auth.vo.RegisterVo;
import com.slowloris.auth.vo.TokenVo;
import com.slowloris.common.Result;

import java.util.List;
import java.util.Map;

public interface UserService extends IService<User> {

    Result<String> register(RegisterVo registerVo);

    Result<Map<String, Object>> login(LoginVo loginVo);

    User getUserByUsername(String username);

    TokenVo refreshToken(String refreshToken);

    Result<Map<String, Object>> verifyToken(String authHeader);

    Result<List<Map<String, Object>>> getUserList();

    Result<String> createUser(Map<String, Object> params);

    Result<String> updateUser(Long id, Map<String, Object> params);

    Result<String> deleteUser(Long id);

    Result<String> resetPassword(String username, String newPassword);

    /** 获取当前登录用户的个人资料（含头像） */
    Result<Map<String, Object>> getProfile(String authHeader);

    /** 更新当前登录用户的个人资料（用户名/邮箱/密码/头像） */
    Result<Map<String, Object>> updateProfile(String authHeader, Map<String, Object> params);
}
