package com.slowloris.log.controller;

import com.slowloris.common.Result;
import com.slowloris.log.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/")
public class LogController {

    @Autowired
    private LogService logService;

    @GetMapping("/logs/list")
    public Result<Map<String, Object>> getLogList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer limit,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String start_time,
            @RequestParam(required = false) String end_time) {
        return logService.getLogList(page, limit, level, module, start_time, end_time);
    }

    @PostMapping("/logs/search")
    public Result<Map<String, Object>> searchLogs(@RequestBody Map<String, Object> params) {
        return logService.searchLogsWithPage(params);
    }

    @GetMapping("/logs/export")
    public Result<String> exportLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return logService.exportLogs(level, module, startTime, endTime);
    }

}
