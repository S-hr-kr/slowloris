package com.slowloris.auth.vo;

import lombok.Data;

/**
 * 登录请求参数
 */
@Data
public class LoginVo {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 是否记住我
     */
    private Boolean rememberMe;

}