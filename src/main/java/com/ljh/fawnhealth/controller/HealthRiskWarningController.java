package com.ljh.fawnhealth.controller;

import com.ljh.fawnhealth.model.entity.HealthRiskWarning;
import com.ljh.fawnhealth.service.HealthRiskWarningService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/health-warnings")
public class HealthRiskWarningController {

    private final HealthRiskWarningService service;

    public HealthRiskWarningController(HealthRiskWarningService service) {
        this.service = service;
    }

    @GetMapping("/user/{userId}")
    public List<HealthRiskWarning> listByUser(@PathVariable Long userId) {
        return service.listByUser(userId);
    }

    @GetMapping("/user/{userId}/range")
    public List<HealthRiskWarning> listByUserAndRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return service.listByUserAndTimeRange(userId, start, end);
    }

    @GetMapping("/unprocessed")
    public List<HealthRiskWarning> listUnprocessed() {
        return service.listUnprocessed();
    }
}