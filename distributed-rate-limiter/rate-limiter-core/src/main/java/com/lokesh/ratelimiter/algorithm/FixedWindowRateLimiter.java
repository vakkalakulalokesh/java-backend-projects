package com.lokesh.ratelimiter.algorithm;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class FixedWindowRateLimiter {

    private static final String LUA = """
            local baseKey = KEYS[1]
            local maxReq = tonumber(ARGV[1])
            local windowMs = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local ttlBufferSec = tonumber(ARGV[4])

            local windowId = math.floor(now / windowMs)
            local rk = baseKey .. ':' .. windowId

            local count = redis.call('INCR', rk)
            if count == 1 then
              redis.call('EXPIRE', rk, ttlBufferSec)
            end

            local allowed = 0
            if count <= maxReq then
              allowed = 1
            end

            local remaining = maxReq - count
            if remaining < 0 then
              remaining = 0
            end

            local retryAfter = 0
            if allowed == 0 then
              local expireAt = (windowId + 1) * windowMs
              retryAfter = math.ceil(expireAt - now)
              if retryAfter < 0 then
                retryAfter = 0
              end
            end

            return {allowed, remaining, retryAfter, maxReq, windowMs}
            """;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> script;

    public FixedWindowRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        DefaultRedisScript<List> s = new DefaultRedisScript<>();
        s.setScriptText(LUA);
        s.setResultType(List.class);
        this.script = s;
    }

    public RateLimiter bind(RateLimiterConfig config) {
        config.validateFor(RateLimiterType.FIXED_WINDOW);
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
                int ttlSec = (int) Math.max(1L, (cfg.getWindowSizeMs() / 1000) + 5L);
                String redisKey = "rl:fw:" + key;
                List<?> raw = redisTemplate.execute(
                        script,
                        Collections.singletonList(redisKey),
                        String.valueOf(cfg.getMaxRequests()),
                        String.valueOf(cfg.getWindowSizeMs()),
                        String.valueOf(now),
                        String.valueOf(ttlSec)
                );
                return LuaResultMapper.toResult(raw);
            }

            @Override
            public RateLimiterType getType() {
                return RateLimiterType.FIXED_WINDOW;
            }
        };
    }
}
