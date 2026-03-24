package com.lokesh.ratelimiter.algorithm;

public record RateLimitResult(
        boolean allowed,
        long remainingTokens,
        long retryAfterMs,
        long limit,
        long windowSizeMs
) {
}
