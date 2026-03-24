package com.lokesh.ratelimiter.exception;

import com.lokesh.ratelimiter.algorithm.RateLimitResult;
import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {

    private final RateLimitResult result;

    public RateLimitExceededException(RateLimitResult result) {
        super("Rate limit exceeded");
        this.result = result;
    }
}
