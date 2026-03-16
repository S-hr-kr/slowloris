package com.slowloris.alert.controller;

import com.slowloris.alert.service.AlertService;
import com.slowloris.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/alerts")
public class AlertController {

    @Autowired
    private AlertService alertService;

    @GetMapping("/list")
    public Result<Map<String, Object>> getAlertList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status) {
        return alertService.getAlertList(page, limit, severity, status);
    }

    @GetMapping("")
    public Result<Map<String, Object>> getAlertListBackwardCompatible(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status) {
        return alertService.getAlertList(page, limit, severity, status);
    }

    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> getAlertDetail(@PathVariable Long id) {
        return alertService.getAlertDetail(id);
    }

    @PutMapping("/handle/{id}")
    public Result<Map<String, Object>> handleAlert(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        return alertService.handleAlert(id, params);
    }

}
