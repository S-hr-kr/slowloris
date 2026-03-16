package com.slowloris.report.controller;

import com.slowloris.common.Result;
import com.slowloris.report.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/reports/list")
    public Result<Map<String, Object>> getReportList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) String type) {
        return reportService.getReportList(page, limit, type);
    }

    @PostMapping("/reports/generate")
    public Result<Map<String, Object>> generateReport(@RequestBody Map<String, Object> params) {
        return reportService.generateReport(params);
    }

    @GetMapping("/reports/download/{id}")
    public Result<String> downloadReport(@PathVariable Long id) {
        return reportService.downloadReport(id);
    }

}