package com.lokesh.gateway.filter;

import com.lokesh.gateway.service.AnalyticsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@RequiredArgsConstructor
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String ATTR_START_MS = "gw.request.startMs";
    public static final String ATTR_METRICS_KEY = "gw.metrics.key";

    private final AnalyticsService analyticsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        request.setAttribute(ATTR_START_MS, System.currentTimeMillis());
        try {
            filterChain.doFilter(request, response);
        } finally {
            Long start = (Long) request.getAttribute(ATTR_START_MS);
            long duration = start == null ? 0L : System.currentTimeMillis() - start;
            String metricsKey = (String) request.getAttribute(ATTR_METRICS_KEY);
            if (metricsKey == null) {
                metricsKey = request.getMethod() + " " + request.getRequestURI();
            }
            boolean allowed = response.getStatus() != 429;
            analyticsService.recordRequest(metricsKey, duration, allowed);
            log.info("{} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
        }
    }
}
