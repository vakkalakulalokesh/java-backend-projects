package com.lokesh.ratelimiter.algorithm;

public enum RateLimiterType {
    TOKEN_BUCKET,
    SLIDING_WINDOW_LOG,
    SLIDING_WINDOW_COUNTER,
    FIXED_WINDOW,
    LEAKY_BUCKET
}
