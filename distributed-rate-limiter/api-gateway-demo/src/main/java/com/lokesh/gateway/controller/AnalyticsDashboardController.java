package com.lokesh.gateway.controller;

import com.lokesh.gateway.model.RateLimitMetrics;
import com.lokesh.gateway.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Rate limiting observability")
public class AnalyticsDashboardController {

    private final AnalyticsService analyticsService;

    @GetMapping("/metrics")
    @Operation(summary = "All metrics")
    public List<RateLimitMetrics> all() {
        return analyticsService.getAllMetrics();
    }

    @GetMapping("/metrics/{key}")
    @Operation(summary = "Metrics for a logical key")
    public RateLimitMetrics one(@PathVariable String key) {
        return analyticsService.getMetrics(URLDecoder.decode(key, StandardCharsets.UTF_8));
    }

    @GetMapping("/top-limited")
    @Operation(summary = "Top rate-limited endpoints by rejections")
    public List<RateLimitMetrics> top() {
        return analyticsService.getTopRateLimitedEndpoints();
    }
}
