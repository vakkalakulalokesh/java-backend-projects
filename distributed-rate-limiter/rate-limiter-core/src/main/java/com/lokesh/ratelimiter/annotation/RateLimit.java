package com.lokesh.ratelimiter.annotation;

import com.lokesh.ratelimiter.algorithm.RateLimiterType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int maxRequests() default 100;

    long windowMs() default 60000;

    RateLimiterType algorithm() default RateLimiterType.SLIDING_WINDOW_COUNTER;

    String key() default "";
}
