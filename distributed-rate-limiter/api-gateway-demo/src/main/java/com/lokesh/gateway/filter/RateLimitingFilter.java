package com.lokesh.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.gateway.dto.ErrorResponse;
import com.lokesh.gateway.model.ApiRoute;
import com.lokesh.gateway.service.RouteService;
import com.lokesh.ratelimiter.algorithm.RateLimitResult;
import com.lokesh.ratelimiter.algorithm.RateLimiter;
import com.lokesh.ratelimiter.algorithm.RateLimiterConfig;
import com.lokesh.ratelimiter.algorithm.RateLimiterFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    public static final String ATTR_REJECTED = "gw.rateLimit.rejected";

    private final RouteService routeService;
    private final RateLimiterFactory rateLimiterFactory;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/h2-console")
                || uri.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        var routeOpt = routeService.findActiveRoute(path, method);
        if (routeOpt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        ApiRoute route = routeOpt.get();
        request.setAttribute(RequestLoggingFilter.ATTR_METRICS_KEY, metricsKey(route, request));

        RateLimiter limiter = limiters.computeIfAbsent(
                limiterCacheKey(route),
                k -> rateLimiterFactory.create(route.getRateLimitAlgorithm(), configFromRoute(route)));

        RateLimitResult result = limiter.tryAcquire(clientKey(request));
        writeHeaders(response, result);
        if (!result.allowed()) {
            request.setAttribute(ATTR_REJECTED, true);
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse body = new ErrorResponse(
                    Instant.now(),
                    429,
                    "Too Many Requests",
                    "Rate limit exceeded for configured route",
                    path);
            objectMapper.writeValue(response.getOutputStream(), body);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String metricsKey(ApiRoute route, HttpServletRequest request) {
        return route.getMethod().toUpperCase() + " " + route.getPath();
    }

    private String limiterCacheKey(ApiRoute route) {
        return route.getId() + "|" + route.getRateLimitAlgorithm().name()
                + "|" + route.getRateLimitMaxRequests() + "|" + route.getRateLimitWindowMs();
    }

    private RateLimiterConfig configFromRoute(ApiRoute route) {
        int max = route.getRateLimitMaxRequests();
        long window = route.getRateLimitWindowMs();
        double windowSeconds = Math.max(window / 1000.0, 1e-3);
        RateLimiterConfig.RateLimiterConfigBuilder b = RateLimiterConfig.builder()
                .maxRequests(max)
                .windowSizeMs(window);
        return switch (route.getRateLimitAlgorithm()) {
            case TOKEN_BUCKET -> b.maxTokens(max).refillRate(max / windowSeconds).build();
            case LEAKY_BUCKET -> b.bucketSize(max).leakRate(max / windowSeconds).build();
            case SLIDING_WINDOW_LOG, SLIDING_WINDOW_COUNTER, FIXED_WINDOW -> b.build();
        };
    }

    private String clientKey(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remainingTokens()));
        if (result.retryAfterMs() > 0) {
            response.setHeader("X-RateLimit-Retry-After", String.valueOf(result.retryAfterMs()));
        }
    }
}
