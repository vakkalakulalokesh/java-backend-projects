package com.lokesh.ratelimiter.algorithm;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class SlidingWindowLogRateLimiter {

    private static final String LUA = """
            local key = KEYS[1]
            local maxReq = tonumber(ARGV[1])
            local windowStart = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local windowMs = tonumber(ARGV[4])
            local ttlSec = tonumber(ARGV[5])
            local member = ARGV[6]

            redis.call('ZREMRANGEBYSCORE', key, '-inf', windowStart)
            local count = redis.call('ZCARD', key)

            local allowed = 0
            local retryAfter = 0

            if count < maxReq then
              redis.call('ZADD', key, now, member)
              allowed = 1
            end

            redis.call('EXPIRE', key, ttlSec)

            local finalCount = redis.call('ZCARD', key)
            local remaining = maxReq - finalCount
            if remaining < 0 then
              remaining = 0
            end

            if allowed == 0 then
              local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
              if oldest[2] ~= false and oldest[2] ~= nil then
                local oldestTs = tonumber(oldest[2])
                retryAfter = math.ceil(oldestTs + windowMs - now)
                if retryAfter < 0 then
                  retryAfter = 0
                end
              else
                retryAfter = math.ceil(windowMs)
              end
            end

            return {allowed, remaining, retryAfter, maxReq, windowMs}
            """;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> script;

    public SlidingWindowLogRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        DefaultRedisScript<List> s = new DefaultRedisScript<>();
        s.setScriptText(LUA);
        s.setResultType(List.class);
        this.script = s;
    }

    public RateLimiter bind(RateLimiterConfig config) {
        config.validateFor(RateLimiterType.SLIDING_WINDOW_LOG);
        return new RateLimiter() {
            @Override
            public RateLimitResult tryAcquire(String key) {
                return tryAcquire(key, 1);
            }

            @Override
            public RateLimitResult tryAcquire(String key, int permits) {
                if (permits <= 0) {
                    throw new IllegalArgumentException("permits must be positive");
                }
                RateLimitResult last = null;
                for (int i = 0; i < permits; i++) {
                    last = singleTry(key, config);
                    if (!last.allowed()) {
                        return last;
                    }
                }
                return last;
            }

            private RateLimitResult singleTry(String key, RateLimiterConfig cfg) {
                long now = System.currentTimeMillis();
                long windowStart = now - cfg.getWindowSizeMs();
                int ttlSec = (int) Math.max(1L, (cfg.getWindowSizeMs() / 1000) + 60L);
                String redisKey = "rl:swl:" + key;
                String member = now + ":" + UUID.randomUUID();
                List<?> raw = redisTemplate.execute(
                        script,
                        Collections.singletonList(redisKey),
                        String.valueOf(cfg.getMaxRequests()),
                        String.valueOf(windowStart),
                        String.valueOf(now),
                        String.valueOf(cfg.getWindowSizeMs()),
                        String.valueOf(ttlSec),
                        member
                );
                return LuaResultMapper.toResult(raw);
            }

            @Override
            public RateLimiterType getType() {
                return RateLimiterType.SLIDING_WINDOW_LOG;
            }
        };
    }
}
