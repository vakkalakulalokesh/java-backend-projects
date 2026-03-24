package com.lokesh.fraud.alert.dto;

import com.lokesh.fraud.common.enums.RiskLevel;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record DashboardStats(
        long totalAlerts,
        long newAlerts,
        long criticalAlerts,
        long resolvedToday,
        double avgRiskScore,
        Map<RiskLevel, Long> alertsByRiskLevel,
        List<DailyCount> alertsTrend
) {
}
