package org.temporedata.modules.v1.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.temporedata.api.contract.ApiResponse;

import java.util.Map;

/**
 * v1 liveness/readiness probe. {@code /api/v1/health} is permitAll; {@code ping} requires auth.
 */
@RestController
@RequestMapping("/api/v1")
public class V1HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of("status", "UP", "service", "temporedata"));
    }

    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping() {
        return ApiResponse.ok(Map.of("pong", true, "at", System.currentTimeMillis()));
    }
}