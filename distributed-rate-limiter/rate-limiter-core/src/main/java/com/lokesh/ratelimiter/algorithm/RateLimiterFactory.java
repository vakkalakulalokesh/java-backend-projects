package com.lokesh.ratelimiter.algorithm;

import org.springframework.stereotype.Component;

@Component
public class RateLimiterFactory {

    private final TokenBucketRateLimiter tokenBucketRateLimiter;
    private final SlidingWindowLogRateLimiter slidingWindowLogRateLimiter;
    private final SlidingWindowCounterRateLimiter slidingWindowCounterRateLimiter;
    private final FixedWindowRateLimiter fixedWindowRateLimiter;
    private final LeakyBucketRateLimiter leakyBucketRateLimiter;

    public RateLimiterFactory(
            TokenBucketRateLimiter tokenBucketRateLimiter,
            SlidingWindowLogRateLimiter slidingWindowLogRateLimiter,
            SlidingWindowCounterRateLimiter slidingWindowCounterRateLimiter,
            FixedWindowRateLimiter fixedWindowRateLimiter,
            LeakyBucketRateLimiter leakyBucketRateLimiter) {
        this.tokenBucketRateLimiter = tokenBucketRateLimiter;
        this.slidingWindowLogRateLimiter = slidingWindowLogRateLimiter;
        this.slidingWindowCounterRateLimiter = slidingWindowCounterRateLimiter;
        this.fixedWindowRateLimiter = fixedWindowRateLimiter;
        this.leakyBucketRateLimiter = leakyBucketRateLimiter;
    }

    public RateLimiter create(RateLimiterType type, RateLimiterConfig config) {
        return switch (type) {
            case TOKEN_BUCKET -> tokenBucketRateLimiter.bind(config);
            case SLIDING_WINDOW_LOG -> slidingWindowLogRateLimiter.bind(config);
            case SLIDING_WINDOW_COUNTER -> slidingWindowCounterRateLimiter.bind(config);
            case FIXED_WINDOW -> fixedWindowRateLimiter.bind(config);
            case LEAKY_BUCKET -> leakyBucketRateLimiter.bind(config);
        };
    }

}
