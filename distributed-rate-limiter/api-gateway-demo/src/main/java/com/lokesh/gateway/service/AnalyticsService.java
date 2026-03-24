package com.lokesh.gateway.service;

import com.lokesh.gateway.model.RateLimitMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final String METRICS_PREFIX = "gw:metrics:";
    private static final String REJECT_RANK = "gw:metrics:reject-rank";

    private final StringRedisTemplate redisTemplate;

    public void recordRequest(String key, long durationMs, boolean allowed) {
        String h = METRICS_PREFIX + sanitize(key);
        redisTemplate.opsForHash().put(h, "_key", key);
        redisTemplate.opsForHash().increment(h, "total", 1);
        if (allowed) {
            redisTemplate.opsForHash().increment(h, "allowed", 1);
        } else {
            redisTemplate.opsForHash().increment(h, "rejected", 1);
            redisTemplate.opsForZSet().incrementScore(REJECT_RANK, key, 1);
        }
        redisTemplate.opsForHash().increment(h, "sumRt", durationMs);
        redisTemplate.opsForHash().increment(h, "rtCount", 1);
        redisTemplate.expire(h, java.time.Duration.ofDays(7));
    }

    public RateLimitMetrics getMetrics(String key) {
        String h = METRICS_PREFIX + sanitize(key);
        var entries = redisTemplate.opsForHash().entries(h);
        if (entries.isEmpty()) {
            return RateLimitMetrics.builder()
                    .key(key)
                    .totalRequests(0)
                    .allowedRequests(0)
                    .rejectedRequests(0)
                    .avgResponseTimeMs(0)
                    .build();
        }
        long total = toLong(entries.get("total"));
        long allowed = toLong(entries.get("allowed"));
        long rejected = toLong(entries.get("rejected"));
        long sumRt = toLong(entries.get("sumRt"));
        long rtCount = toLong(entries.get("rtCount"));
        double avg = rtCount == 0 ? 0.0 : (double) sumRt / rtCount;
        return RateLimitMetrics.builder()
                .key(key)
                .totalRequests(total)
                .allowedRequests(allowed)
                .rejectedRequests(rejected)
                .avgResponseTimeMs(avg)
                .build();
    }

    public List<RateLimitMetrics> getAllMetrics() {
        Set<String> keys = redisTemplate.keys(METRICS_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        List<RateLimitMetrics> list = new ArrayList<>();
        for (String k : keys) {
            Object rawKey = redisTemplate.opsForHash().get(k, "_key");
            String logical = rawKey != null ? rawKey.toString() : k.substring(METRICS_PREFIX.length());
            list.add(getMetrics(logical));
        }
        return list;
    }

    public List<RateLimitMetrics> getTopRateLimitedEndpoints() {
        Set<ZSetOperations.TypedTuple<String>> top =
                redisTemplate.opsForZSet().reverseRangeWithScores(REJECT_RANK, 0, 9);
        if (top == null) {
            return List.of();
        }
        List<RateLimitMetrics> result = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> t : top) {
            if (t.getValue() == null) {
                continue;
            }
            RateLimitMetrics base = getMetrics(t.getValue());
            result.add(RateLimitMetrics.builder()
                    .key(base.getKey())
                    .totalRequests(base.getTotalRequests())
                    .allowedRequests(base.getAllowedRequests())
                    .rejectedRequests(base.getRejectedRequests())
                    .avgResponseTimeMs(base.getAvgResponseTimeMs())
                    .build());
        }
        return result.isEmpty() ? Collections.emptyList() : result;
    }

    private static String sanitize(String key) {
        return Objects.requireNonNull(key).replaceAll("[^a-zA-Z0-9._:-]", "_");
    }

    private static long toLong(Object v) {
        if (v == null) {
            return 0L;
        }
        if (v instanceof Long l) {
            return l;
        }
        if (v instanceof Integer i) {
            return i.longValue();
        }
        if (v instanceof String s) {
            return Long.parseLong(s);
        }
        if (v instanceof byte[] b) {
            return Long.parseLong(new String(b));
        }
        return 0L;
    }
}
