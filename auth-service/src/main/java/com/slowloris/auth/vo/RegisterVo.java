package com.slowloris.auth.vo;

import lombok.Data;

/**
 * 注册请求参数
 */
@Data
public class RegisterVo {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 用户角色
     */
    private String role;

    /**
     * 邮箱
     */
    private String email;

}