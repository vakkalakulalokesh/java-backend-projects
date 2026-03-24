package com.lokesh.ratelimiter.algorithm;

public interface RateLimiter {
    RateLimitResult tryAcquire(String key);

    RateLimitResult tryAcquire(String key, int permits);

    RateLimiterType getType();
}
