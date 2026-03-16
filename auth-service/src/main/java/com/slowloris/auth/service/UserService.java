package com.slowloris.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.slowloris.auth.entity.User;
import com.slowloris.auth.vo.LoginVo;
import com.slowloris.auth.vo.RegisterVo;
import com.slowloris.auth.vo.TokenVo;
import com.slowloris.common.Result;

import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param registerVo 注册信息
     * @return 注册结果
     */
    Result<Map<String, Object>> register(RegisterVo registerVo);

    /**
     * 用户登录
     * @param loginVo 登录信息
     * @return 登录结果
     */
    Result<Map<String, Object>> login(LoginVo loginVo);

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    User getUserByUsername(String username);

    /**
     * 刷新令牌
     * @param refreshToken 刷新令牌
     * @return 新的令牌信息
     */
    TokenVo refreshToken(String refreshToken);

    /**
     * 验证令牌
     * @param authHeader 认证头
     * @return 验证结果
     */
    Result<Map<String, Object>> verifyToken(String authHeader);

    /**
     * 获取用户列表
     * @param page 页码
     * @param limit 每页数量
     * @param role 用户角色
     * @return 用户列表
     */
    Result<Map<String, Object>> getUserList(Integer page, Integer limit, String role);

    /**
     * 创建用户
     * @param params 请求参数
     * @return 创建结果
     */
    Result<Map<String, Object>> createUser(Map<String, Object> params);

    /**
     * 更新用户
     * @param id 用户ID
     * @param params 请求参数
     * @return 更新结果
     */
    Result<Map<String, Object>> updateUser(Long id, Map<String, Object> params);

    /**
     * 删除用户
     * @param id 用户ID
     * @return 删除结果
     */
    Result<String> deleteUser(Long id);

}