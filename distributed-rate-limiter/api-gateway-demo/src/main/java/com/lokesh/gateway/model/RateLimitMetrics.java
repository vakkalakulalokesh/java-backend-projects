package com.lokesh.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitMetrics {

    private String key;
    private long totalRequests;
    private long allowedRequests;
    private long rejectedRequests;
    private double avgResponseTimeMs;
}
