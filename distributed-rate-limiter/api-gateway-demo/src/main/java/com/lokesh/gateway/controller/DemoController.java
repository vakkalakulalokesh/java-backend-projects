package com.lokesh.gateway.controller;

import com.lokesh.ratelimiter.algorithm.RateLimiterType;
import com.lokesh.ratelimiter.annotation.RateLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/demo")
@Tag(name = "Demo", description = "Sample endpoints for exercising rate limiting")
public class DemoController {

    @GetMapping("/public")
    @Operation(summary = "Public endpoint without rate limiting")
    public Map<String, String> publicEndpoint() {
        return Map.of("message", "unlimited");
    }

    @GetMapping("/limited")
    @RateLimit(maxRequests = 10, windowMs = 60_000, algorithm = RateLimiterType.TOKEN_BUCKET)
    @Operation(summary = "Token bucket (10 / minute)")
    public Map<String, String> limited() {
        return Map.of("message", "token bucket limited");
    }

    @GetMapping("/strict")
    @RateLimit(maxRequests = 3, windowMs = 30_000, algorithm = RateLimiterType.SLIDING_WINDOW_LOG)
    @Operation(summary = "Sliding window log (3 / 30s)")
    public Map<String, String> strict() {
        return Map.of("message", "strict sliding log");
    }

    @GetMapping("/standard")
    @RateLimit(maxRequests = 20, windowMs = 60_000, algorithm = RateLimiterType.FIXED_WINDOW)
    @Operation(summary = "Fixed window (20 / minute)")
    public Map<String, String> standard() {
        return Map.of("message", "fixed window");
    }
}
