package com.slowloris.auth.service;

import com.alibaba.fastjson2.JSON;
import com.slowloris.auth.entity.WxUser;
import com.slowloris.auth.mapper.WxUserMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class WeChatService {

    @Value("${wechat.app-id}")
    private String appId;

    @Value("${wechat.app-secret}")
    private String appSecret;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private WxUserMapper wxUserMapper;

    public String handleMockCallback(String code) {
        // 个人号无 snsapi_login 权限，用固定 mock openid 模拟一个微信用户，跑通整条登录链路。
        String openid = "mock_openid_demo";
        WxUser wxUser = wxUserMapper.findByOpenid(openid);
        if (wxUser == null) {
            wxUser = new WxUser();
            wxUser.setOpenid(openid);
            wxUser.setNickname("微信用户(模拟)");
            wxUserMapper.insert(wxUser);
        }
        return issueToken(wxUser, openid);
    }

    public String handleCallback(String code) {
        // 1. 用 code 换取 access_token 和 openid
        String tokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token"
                + "?appid=" + appId
                + "&secret=" + appSecret
                + "&code=" + code
                + "&grant_type=authorization_code";

        Map<?, ?> tokenResp = restTemplate.getForObject(tokenUrl, Map.class);
        if (tokenResp == null) throw new RuntimeException("微信接口无响应");

        String openid = (String) tokenResp.get("openid");
        String accessToken = (String) tokenResp.get("access_token");
        if (openid == null) {
            throw new RuntimeException("微信授权失败: " + tokenResp.get("errmsg"));
        }

        // 2. 获取用户信息
        String userInfoUrl = "https://api.weixin.qq.com/sns/userinfo"
                + "?access_token=" + accessToken
                + "&openid=" + openid
                + "&lang=zh_CN";
        Map<?, ?> userInfo = restTemplate.getForObject(userInfoUrl, Map.class);

        // 3. 创建或更新微信用户记录
        WxUser wxUser = wxUserMapper.findByOpenid(openid);
        if (wxUser == null) {
            wxUser = new WxUser();
            wxUser.setOpenid(openid);
        }
        if (userInfo != null) {
            wxUser.setNickname((String) userInfo.get("nickname"));
            wxUser.setAvatarUrl((String) userInfo.get("headimgurl"));
        }
        if (wxUser.getId() == null) {
            wxUserMapper.insert(wxUser);
        } else {
            wxUserMapper.updateById(wxUser);
        }

        // 4. 签发 JWT
        return issueToken(wxUser, openid);
    }

    private String issueToken(WxUser wxUser, String openid) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", wxUser.getId().toString());
        claims.put("username", "wx_" + openid.substring(0, Math.min(8, openid.length())));
        claims.put("roles", List.of("ROLE_USER"));
        claims.put("loginType", "wechat");

        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtExpiration))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .compact();
    }
}
