package com.lokesh.ratelimiter.aspect;

import com.lokesh.ratelimiter.algorithm.RateLimitResult;
import com.lokesh.ratelimiter.algorithm.RateLimiter;
import com.lokesh.ratelimiter.algorithm.RateLimiterConfig;
import com.lokesh.ratelimiter.algorithm.RateLimiterFactory;
import com.lokesh.ratelimiter.algorithm.RateLimiterType;
import com.lokesh.ratelimiter.annotation.RateLimit;
import com.lokesh.ratelimiter.exception.RateLimitExceededException;
import com.lokesh.ratelimiter.properties.RateLimiterProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class RateLimitAspect {

    private static final ParameterNameDiscoverer PARAMETER_NAMES = new DefaultParameterNameDiscoverer();

    private final RateLimiterFactory rateLimiterFactory;
    private final RateLimiterProperties properties;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ConcurrentHashMap<String, RateLimiter> limiterCache = new ConcurrentHashMap<>();

    public RateLimitAspect(
            RateLimiterFactory rateLimiterFactory,
            @Autowired(required = false) @Nullable RateLimiterProperties properties) {
        this.rateLimiterFactory = rateLimiterFactory;
        this.properties = properties;
    }

    @Around("@annotation(rateLimit)")
    public Object enforce(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = attrs.getRequest();
        HttpServletResponse response = attrs.getResponse();

        String clientKey = resolveKey(joinPoint, rateLimit, request);
        RateLimiterConfig config = buildConfig(rateLimit);
        RateLimiter limiter = limiterCache.computeIfAbsent(
                cacheKey(rateLimit),
                k -> rateLimiterFactory.create(rateLimit.algorithm(), config));

        RateLimitResult result = limiter.tryAcquire(clientKey);
        if (!result.allowed()) {
            throw new RateLimitExceededException(result);
        }
        if (response != null) {
            writeHeaders(response, result);
        }
        return joinPoint.proceed();
    }

    private String cacheKey(RateLimit rl) {
        return rl.algorithm().name() + '|' + rl.maxRequests() + '|' + rl.windowMs();
    }

    private RateLimiterConfig buildConfig(RateLimit rl) {
        int max = rl.maxRequests() > 0 ? rl.maxRequests()
                : (properties != null ? properties.getDefaultMaxRequests() : 100);
        long window = rl.windowMs() > 0 ? rl.windowMs()
                : (properties != null ? properties.getDefaultWindowMs() : 60_000L);

        RateLimiterConfig.RateLimiterConfigBuilder builder = RateLimiterConfig.builder()
                .maxRequests(max)
                .windowSizeMs(window);

        double windowSeconds = window / 1000.0;
        if (windowSeconds <= 0) {
            windowSeconds = 1.0;
        }

        return switch (rl.algorithm()) {
            case TOKEN_BUCKET -> builder
                    .maxTokens(max)
                    .refillRate(max / windowSeconds)
                    .build();
            case LEAKY_BUCKET -> builder
                    .bucketSize(max)
                    .leakRate(max / windowSeconds)
                    .build();
            case SLIDING_WINDOW_LOG, SLIDING_WINDOW_COUNTER, FIXED_WINDOW -> builder.build();
        };
    }

    private String resolveKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit, HttpServletRequest request) {
        String expr = rateLimit.key();
        if (expr == null || expr.isBlank()) {
            return clientIp(request);
        }
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        EvaluationContext ctx = new StandardEvaluationContext();
        ctx.setVariable("request", request);
        Object[] args = joinPoint.getArgs();
        String[] names = PARAMETER_NAMES.getParameterNames(method);
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                ctx.setVariable(names[i], args[i]);
            }
        }
        Object value = expressionParser.parseExpression(expr).getValue(ctx);
        return value != null ? value.toString() : clientIp(request);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public static void writeHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remainingTokens()));
        if (result.retryAfterMs() > 0) {
            response.setHeader("X-RateLimit-Retry-After", String.valueOf(result.retryAfterMs()));
        }
    }
}
