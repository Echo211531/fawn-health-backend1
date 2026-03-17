package com.zr.health.controller;

import com.zr.health.commen.BaseResponse;
import com.zr.health.config.ResultUtils;
import com.zr.health.manager.sse.SseEmitterManager;
import com.zr.health.model.entity.HealthRiskWarning;
import com.zr.health.service.HealthRiskWarningService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/health-warnings")
public class HealthRiskWarningController {

    @Resource
    private HealthRiskWarningService service;

    @Resource
    private SseEmitterManager sseEmitterManager;

    @GetMapping("/user/{userId}")
    public BaseResponse<List<HealthRiskWarning>> listByUser(@PathVariable Long userId) {
        return ResultUtils.success(service.listByUser(userId));
    }

    @GetMapping("/user/{userId}/range")
    public BaseResponse<List<HealthRiskWarning>> listByUserAndRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResultUtils.success(service.listByUserAndTimeRange(userId, start, end));
    }

    @GetMapping("/unprocessed")
    public BaseResponse<List<HealthRiskWarning>> listUnprocessed() {
        return ResultUtils.success(service.listUnprocessed());
    }

    /**
     * 查询指定用户未处理预警数量
     */
    @GetMapping("/unprocessed/count")
    public BaseResponse<Long> countUnprocessedByUser(@RequestParam Long userId) {
        long count = service.countUnprocessedByUser(userId);
        return ResultUtils.success(count);
    }

    /**
     * 建立SSE连接，实时获取用户最新预警记录
     * 
     * @param userId 用户ID
     * @return SSE连接对象
     */
    @GetMapping("/sse/subscribe/{userId}")
    public SseEmitter subscribeToWarnings(@PathVariable Long userId) {
        // 创建SSE连接并存储
        SseEmitter emitter = sseEmitterManager.createEmitter(userId);

        // 可选：连接建立后立即推送最近的1条未处理预警（优化用户体验）
        List<HealthRiskWarning> latestWarnings = service.getLatestUnprocessedByUser(userId, 1);
        if (!latestWarnings.isEmpty()) {
            try {
                emitter.send(SseEmitter.event()
                        .name("initialWarning")
                        .data(latestWarnings.get(0)));
            } catch (IOException e) {
                // 发送初始数据失败不影响连接建立
                e.printStackTrace();
                log.info("sse获取数据失败");
            }
        }

        return emitter;
    }

    /**
     * 标记预警为已处理
     */
    @PostMapping("/{id}/process")
    public BaseResponse<Boolean> processWarning(@PathVariable Long id) {
        boolean ok = service.markProcessed(id);
        return ResultUtils.success(ok);
    }
}