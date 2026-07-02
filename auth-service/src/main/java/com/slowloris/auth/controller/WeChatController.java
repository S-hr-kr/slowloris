package com.slowloris.auth.controller;

import com.slowloris.auth.service.WeChatService;
import com.slowloris.common.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/wechat")
public class WeChatController {

    @Value("${wechat.app-id}")
    private String appId;

    @Value("${wechat.callback-url}")
    private String callbackUrl;

    @Value("${wechat.mock-enabled:false}")
    private boolean mockEnabled;

    @Autowired
    private WeChatService weChatService;

    @GetMapping("/login")
    public Result<String> getLoginUrl() {
        String state = UUID.randomUUID().toString().replace("-", "");
        // 个人号无 snsapi_login 权限，mock 模式下直接把浏览器导向本地回调，模拟扫码成功。
        if (mockEnabled) {
            String mockUrl = callbackUrl + "?code=MOCK_" + state + "&state=" + state;
            return Result.success(mockUrl, "mock 登录地址生成成功");
        }
        String url = "https://open.weixin.qq.com/connect/qrconnect"
                + "?appid=" + appId
                + "&redirect_uri=" + URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=snsapi_login"
                + "&state=" + state
                + "#wechat_redirect";
        // 注意：必须用两参重载把 url 放进 data 字段，否则 String 会匹配到 success(String message) 重载，
        // 导致 url 进入 message，前端读取 resp.data 为空。
        return Result.success(url, "登录地址生成成功");
    }

    @GetMapping("/callback")
    public void callback(@RequestParam String code,
                         @RequestParam(required = false) String state,
                         HttpServletResponse response) throws IOException {
        try {
            String token = mockEnabled
                    ? weChatService.handleMockCallback(code)
                    : weChatService.handleCallback(code);
            // 前端从 URL query 参数读取 token 存入 localStorage
            response.sendRedirect("/login?wechat_token=" + token);
        } catch (Exception e) {
            log.error("微信回调处理失败", e);
            response.sendRedirect("/login?error=wechat_failed");
        }
    }
}
