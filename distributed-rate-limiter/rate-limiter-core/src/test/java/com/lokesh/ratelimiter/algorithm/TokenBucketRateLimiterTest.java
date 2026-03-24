package com.lokesh.ratelimiter.algorithm;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenBucketRateLimiterTest {

    @Test
    void enforcesPositiveCapacityAndRefill() {
        assertThatCode(() -> RateLimiterConfig.builder()
                .maxTokens(10)
                .refillRate(2.5)
                .build()
                .validateFor(RateLimiterType.TOKEN_BUCKET)).doesNotThrowAnyException();

        assertThatThrownBy(() -> RateLimiterConfig.builder()
                .maxTokens(5)
                .refillRate(0)
                .build()
                .validateFor(RateLimiterType.TOKEN_BUCKET))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void interpretsLuaResponseForAdmission() {
        RateLimitResult allowed = LuaResultMapper.toResult(List.of(1L, 7L, 0L, 10L, 0L));
        assertThat(allowed.allowed()).isTrue();
        assertThat(allowed.remainingTokens()).isEqualTo(7);
        assertThat(allowed.limit()).isEqualTo(10);

        RateLimitResult denied = LuaResultMapper.toResult(List.of(0L, 1L, 200L, 10L, 0L));
        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfterMs()).isEqualTo(200L);
    }
}
