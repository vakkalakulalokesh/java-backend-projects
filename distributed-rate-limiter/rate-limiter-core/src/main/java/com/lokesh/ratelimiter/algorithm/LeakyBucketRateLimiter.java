package com.lokesh.ratelimiter.algorithm;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class LeakyBucketRateLimiter {

    private static final String LUA = """
            local key = KEYS[1]
            local bucketSize = tonumber(ARGV[1])
            local leakRate = tonumber(ARGV[2])
            local permits = tonumber(ARGV[3])
            local now = tonumber(ARGV[4])
            local ttlSec = tonumber(ARGV[5])

            local levelStr = redis.call('HGET', key, 'level')
            local lastStr = redis.call('HGET', key, 'last_ts')

            local level
            local last_ts
            if levelStr == false or levelStr == nil then
              level = 0.0
              last_ts = now
            else
              level = tonumber(levelStr)
              last_ts = tonumber(lastStr)
            end

            local elapsed = (now - last_ts) / 1000.0
            if elapsed > 0 then
              level = math.max(0.0, level - elapsed * leakRate)
              last_ts = now
            end

            local allowed = 0
            local retryAfter = 0

            if level + permits <= bucketSize then
              level = level + permits
              allowed = 1
            else
              local excess = (level + permits) - bucketSize
              retryAfter = math.ceil((excess / leakRate) * 1000)
            end

            redis.call('HSET', key, 'level', tostring(level), 'last_ts', tostring(last_ts))
            redis.call('EXPIRE', key, ttlSec)

            local remaining = math.floor(math.max(0, bucketSize - level))
            return {allowed, remaining, retryAfter, bucketSize, 0}
            """;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> script;

    public LeakyBucketRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        DefaultRedisScript<List> s = new DefaultRedisScript<>();
        s.setScriptText(LUA);
        s.setResultType(List.class);
        this.script = s;
    }

    public RateLimiter bind(RateLimiterConfig config) {
        config.validateFor(RateLimiterType.LEAKY_BUCKET);
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
                long now = System.currentTimeMillis();
                String redisKey = "rl:lb:" + key;
                List<?> raw = redisTemplate.execute(
                        script,
                        Collections.singletonList(redisKey),
                        String.valueOf(config.getBucketSize()),
                        String.valueOf(config.getLeakRate()),
                        String.valueOf(permits),
                        String.valueOf(now),
                        "86400"
                );
                return LuaResultMapper.toResult(raw);
            }

            @Override
            public RateLimiterType getType() {
                return RateLimiterType.LEAKY_BUCKET;
            }
        };
    }
}
