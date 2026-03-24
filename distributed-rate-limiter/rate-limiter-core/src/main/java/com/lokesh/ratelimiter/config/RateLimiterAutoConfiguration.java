package com.lokesh.ratelimiter.config;

import com.lokesh.ratelimiter.algorithm.FixedWindowRateLimiter;
import com.lokesh.ratelimiter.algorithm.LeakyBucketRateLimiter;
import com.lokesh.ratelimiter.algorithm.RateLimiterFactory;
import com.lokesh.ratelimiter.algorithm.SlidingWindowCounterRateLimiter;
import com.lokesh.ratelimiter.algorithm.SlidingWindowLogRateLimiter;
import com.lokesh.ratelimiter.algorithm.TokenBucketRateLimiter;
import com.lokesh.ratelimiter.aspect.RateLimitAspect;
import com.lokesh.ratelimiter.properties.RateLimiterProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration(after = RedisAutoConfiguration.class)
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnBean(StringRedisTemplate.class)
@ConditionalOnProperty(prefix = "rate-limiter", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(RateLimiterProperties.class)
@Import({
        TokenBucketRateLimiter.class,
        SlidingWindowLogRateLimiter.class,
        SlidingWindowCounterRateLimiter.class,
        FixedWindowRateLimiter.class,
        LeakyBucketRateLimiter.class,
        RateLimiterFactory.class,
        RateLimitAspect.class
})
public class RateLimiterAutoConfiguration {
}
