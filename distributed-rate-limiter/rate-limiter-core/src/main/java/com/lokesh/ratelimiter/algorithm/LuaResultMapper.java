package com.lokesh.ratelimiter.algorithm;

import java.util.List;

final class LuaResultMapper {

    private LuaResultMapper() {
    }

    static RateLimitResult toResult(List<?> raw) {
        if (raw == null || raw.size() < 5) {
            throw new IllegalStateException("Unexpected Lua script result: " + raw);
        }
        long allowed = toLong(raw.get(0));
        long remaining = toLong(raw.get(1));
        long retryAfter = toLong(raw.get(2));
        long limit = toLong(raw.get(3));
        long window = toLong(raw.get(4));
        return new RateLimitResult(allowed != 0, remaining, retryAfter, limit, window);
    }

    static long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Integer i) {
            return i.longValue();
        }
        if (value instanceof byte[] bytes) {
            return Long.parseLong(new String(bytes));
        }
        if (value instanceof String s) {
            return Long.parseLong(s);
        }
        throw new IllegalArgumentException("Unsupported numeric type: " + value.getClass());
    }
}
