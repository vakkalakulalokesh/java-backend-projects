package com.lokesh.ratelimiter.algorithm;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class TokenBucketRateLimiter {

    private static final String LUA = """
            local key = KEYS[1]
            local maxTokens = tonumber(ARGV[1])
            local refillRate = tonumber(ARGV[2])
            local permits = tonumber(ARGV[3])
            local now = tonumber(ARGV[4])
            local ttlSec = tonumber(ARGV[5])

            local tokensStr = redis.call('HGET', key, 'tokens')
            local lastStr = redis.call('HGET', key, 'last_ts')

            local tokens
            local last_ts
            if tokensStr == false or tokensStr == nil then
              tokens = maxTokens
              last_ts = now
            else
              tokens = tonumber(tokensStr)
              last_ts = tonumber(lastStr)
            end

            local elapsed = (now - last_ts) / 1000.0
            if elapsed > 0 then
              tokens = math.min(maxTokens, tokens + elapsed * refillRate)
              last_ts = now
            end

            local allowed = 0
            local remaining = math.floor(tokens)
            local retryAfter = 0

            if tokens >= permits then
              tokens = tokens - permits
              allowed = 1
              remaining = math.floor(tokens)
            else
              local deficit = permits - tokens
              retryAfter = math.ceil((deficit / refillRate) * 1000)
            end

            redis.call('HSET', key, 'tokens', tostring(tokens), 'last_ts', tostring(last_ts))
            redis.call('EXPIRE', key, ttlSec)

            return {allowed, remaining, retryAfter, maxTokens, 0}
            """;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> script;

    public TokenBucketRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        DefaultRedisScript<List> s = new DefaultRedisScript<>();
        s.setScriptText(LUA);
        s.setResultType(List.class);
        this.script = s;
    }

    public RateLimiter bind(RateLimiterConfig config) {
        config.validateFor(RateLimiterType.TOKEN_BUCKET);
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
                String redisKey = "rl:tb:" + key;
                List<?> raw = redisTemplate.execute(
                        script,
                        Collections.singletonList(redisKey),
                        String.valueOf(config.getMaxTokens()),
                        String.valueOf(config.getRefillRate()),
                        String.valueOf(permits),
                        String.valueOf(now),
                        "86400"
                );
                return LuaResultMapper.toResult(raw);
            }

            @Override
            public RateLimiterType getType() {
                return RateLimiterType.TOKEN_BUCKET;
            }
        };
    }
}
