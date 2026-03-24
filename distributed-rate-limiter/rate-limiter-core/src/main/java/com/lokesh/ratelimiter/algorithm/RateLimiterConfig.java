package com.lokesh.ratelimiter.algorithm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimiterConfig {

    @Builder.Default
    private int maxRequests = 100;

    @Builder.Default
    private long windowSizeMs = 60_000L;

    @Builder.Default
    private int maxTokens = 100;

    @Builder.Default
    private double refillRate = 1.0;

    @Builder.Default
    private int bucketSize = 100;

    @Builder.Default
    private double leakRate = 1.0;

    @Builder.Default
    private int subWindowCount = 10;

    public void validateFor(RateLimiterType type) {
        switch (type) {
            case TOKEN_BUCKET -> {
                if (maxTokens <= 0) {
                    throw new IllegalArgumentException("maxTokens must be positive");
                }
                if (refillRate <= 0) {
                    throw new IllegalArgumentException("refillRate must be positive");
                }
            }
            case SLIDING_WINDOW_LOG, FIXED_WINDOW -> {
                if (maxRequests <= 0) {
                    throw new IllegalArgumentException("maxRequests must be positive");
                }
                if (windowSizeMs <= 0) {
                    throw new IllegalArgumentException("windowSizeMs must be positive");
                }
            }
            case SLIDING_WINDOW_COUNTER -> {
                if (maxRequests <= 0) {
                    throw new IllegalArgumentException("maxRequests must be positive");
                }
                if (windowSizeMs <= 0) {
                    throw new IllegalArgumentException("windowSizeMs must be positive");
                }
                if (subWindowCount <= 0) {
                    throw new IllegalArgumentException("subWindowCount must be positive");
                }
            }
            case LEAKY_BUCKET -> {
                if (bucketSize <= 0) {
                    throw new IllegalArgumentException("bucketSize must be positive");
                }
                if (leakRate <= 0) {
                    throw new IllegalArgumentException("leakRate must be positive");
                }
            }
        }
    }
}
