package com.lokesh.ratelimiter.algorithm;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlidingWindowCounterRateLimiterTest {

    @Test
    void requiresPositiveSubWindows() {
        assertThatCode(() -> RateLimiterConfig.builder()
                .maxRequests(20)
                .windowSizeMs(60_000)
                .subWindowCount(10)
                .build()
                .validateFor(RateLimiterType.SLIDING_WINDOW_COUNTER)).doesNotThrowAnyException();

        assertThatThrownBy(() -> RateLimiterConfig.builder()
                .maxRequests(20)
                .windowSizeMs(60_000)
                .subWindowCount(-1)
                .build()
                .validateFor(RateLimiterType.SLIDING_WINDOW_COUNTER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapsApproximateWindowMetricsFromScript() {
        RateLimitResult r = LuaResultMapper.toResult(List.of(1L, 12L, 0L, 50L, 120_000L));
        assertThat(r.allowed()).isTrue();
        assertThat(r.remainingTokens()).isEqualTo(12);
        assertThat(r.windowSizeMs()).isEqualTo(120_000L);
    }
}
