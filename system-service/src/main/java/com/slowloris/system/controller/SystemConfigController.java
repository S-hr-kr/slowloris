package com.slowloris.system.controller;

import com.slowloris.common.Result;
import com.slowloris.system.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/")
public class SystemConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    @GetMapping("/system/config")
    public Result<Map<String, Object>> getSystemConfig() {
        return systemConfigService.getSystemConfig();
    }

    @PutMapping("/system/config")
    public Result<Map<String, Object>> updateSystemConfig(@RequestBody Map<String, Object> configData) {
        return systemConfigService.updateSystemConfig(configData);
    }

    @GetMapping("/system/status")
    public Result<Map<String, Object>> getSystemStatus() {
        return systemConfigService.getSystemStatus();
    }

}