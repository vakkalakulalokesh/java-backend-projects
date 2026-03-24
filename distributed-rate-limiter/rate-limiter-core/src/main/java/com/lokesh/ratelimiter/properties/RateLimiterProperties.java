package com.lokesh.ratelimiter.properties;

import com.lokesh.ratelimiter.algorithm.RateLimiterType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    private boolean enabled = true;

    private RateLimiterType defaultAlgorithm = RateLimiterType.SLIDING_WINDOW_COUNTER;

    private int defaultMaxRequests = 100;

    private long defaultWindowMs = 60_000L;

    private Map<String, RuleConfig> rules = new LinkedHashMap<>();

    @Data
    public static class RuleConfig {
        private RateLimiterType algorithm;
        private Integer maxRequests;
        private Long windowMs;
    }
}
