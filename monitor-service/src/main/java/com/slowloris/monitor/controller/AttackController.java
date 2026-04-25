package com.slowloris.monitor.controller;

import com.slowloris.common.Result;
import com.slowloris.monitor.service.AttackService;
import com.slowloris.monitor.service.WebSocketBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AttackController {

    private final AttackService attackService;
    private final WebSocketBroadcastService broadcastService;

    // GET /attacks
    @GetMapping("/attacks")
    public Result<Map<String, Object>> getAttacks() {
        return attackService.getAttacks();
    }

    // POST /attacks/block
    @PostMapping("/attacks/block")
    public Result<String> blockAttack(@RequestBody Map<String, Object> body) {
        Object attackIdObj = body.get("attackId");
        if (attackIdObj == null) {
            return Result.error("attackId不能为空");
        }
        Long attackId = Long.valueOf(attackIdObj.toString());
        Result<String> result = attackService.blockAttack(attackId);
        if (result.isSuccess()) {
            broadcastService.broadcastAttackUpdate(Map.of("attackId", attackId, "status", "blocked"));
        }
        return result;
    }
}
