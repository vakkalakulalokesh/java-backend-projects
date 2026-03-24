package com.lokesh.ratelimiter.algorithm;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class SlidingWindowCounterRateLimiter {

    private static final String LUA = """
            local key = KEYS[1]
            local maxReq = tonumber(ARGV[1])
            local windowMs = tonumber(ARGV[2])
            local subCount = tonumber(ARGV[3])
            local now = tonumber(ARGV[4])
            local ttlSec = tonumber(ARGV[5])

            local subMs = windowMs / subCount

            local fields = redis.call('HGETALL', key)
            for i = 1, #fields, 2 do
              local bid = tonumber(fields[i])
              local finish = bid * subMs + subMs
              if finish <= now - windowMs then
                redis.call('HDEL', key, fields[i])
              end
            end

            local function weighted_total()
              local total = 0.0
              local f = redis.call('HGETALL', key)
              for i = 1, #f, 2 do
                local bid = tonumber(f[i])
                local c = tonumber(f[i + 1])
                local start = bid * subMs
                local finish = start + subMs
                local winStart = now - windowMs
                local overlap = math.min(finish, now) - math.max(start, winStart)
                if overlap > 0 then
                  total = total + c * (overlap / subMs)
                end
              end
              return total
            end

            local before = weighted_total()
            local allowed = 0
            local retryAfter = 0

            if before < maxReq then
              local currentBid = math.floor(now / subMs)
              redis.call('HINCRBY', key, tostring(currentBid), 1)
              allowed = 1
            else
              retryAfter = math.ceil(subMs)
            end

            redis.call('EXPIRE', key, ttlSec)

            local after = weighted_total()
            local remaining = math.floor(math.max(0, maxReq - math.ceil(after)))
            if allowed == 0 then
              remaining = math.floor(math.max(0, maxReq - math.ceil(before)))
            end

            return {allowed, remaining, retryAfter, maxReq, windowMs}
            """;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> script;

    public SlidingWindowCounterRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        DefaultRedisScript<List> s = new DefaultRedisScript<>();
        s.setScriptText(LUA);
        s.setResultType(List.class);
        this.script = s;
    }

    public RateLimiter bind(RateLimiterConfig config) {
        config.validateFor(RateLimiterType.SLIDING_WINDOW_COUNTER);
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

            private RateLimitResult singleTry(String k, RateLimiterConfig cfg) {
                long now = System.currentTimeMillis();
                int ttlSec = (int) Math.max(1L, (cfg.getWindowSizeMs() / 1000) + 120L);
                String redisKey = "rl:swc:" + k;
                List<?> raw = redisTemplate.execute(
                        script,
                        Collections.singletonList(redisKey),
                        String.valueOf(cfg.getMaxRequests()),
                        String.valueOf(cfg.getWindowSizeMs()),
                        String.valueOf(cfg.getSubWindowCount()),
                        String.valueOf(now),
                        String.valueOf(ttlSec)
                );
                return LuaResultMapper.toResult(raw);
            }

            @Override
            public RateLimiterType getType() {
                return RateLimiterType.SLIDING_WINDOW_COUNTER;
            }
        };
    }
}
