package com.lokesh.gateway.model;

import com.lokesh.ratelimiter.algorithm.RateLimiterType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "api_routes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_path", nullable = false)
    private String path;

    @Column(nullable = false)
    private String targetUrl;

    @Column(nullable = false)
    private String method;

    @Column(nullable = false)
    private int rateLimitMaxRequests;

    @Column(nullable = false)
    private long rateLimitWindowMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private RateLimiterType rateLimitAlgorithm;

    @Column(nullable = false)
    private boolean active;

    private String description;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
