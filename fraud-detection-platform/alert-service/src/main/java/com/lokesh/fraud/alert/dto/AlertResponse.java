package com.lokesh.fraud.alert.dto;

import com.lokesh.fraud.alert.entity.AlertEntity;
import com.lokesh.fraud.common.enums.AlertStatus;
import com.lokesh.fraud.common.enums.RiskLevel;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AlertResponse(
        String alertId,
        String transactionId,
        String accountId,
        double riskScore,
        RiskLevel riskLevel,
        String triggeredRules,
        String recommendation,
        AlertStatus status,
        String analystNotes,
        String resolvedBy,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        LocalDateTime updatedAt
) {

    public static AlertResponse fromEntity(AlertEntity e) {
        return AlertResponse.builder()
                .alertId(e.getAlertId())
                .transactionId(e.getTransactionId())
                .accountId(e.getAccountId())
                .riskScore(e.getRiskScore())
                .riskLevel(e.getRiskLevel())
                .triggeredRules(e.getTriggeredRules())
                .recommendation(e.getRecommendation())
                .status(e.getStatus())
                .analystNotes(e.getAnalystNotes())
                .resolvedBy(e.getResolvedBy())
                .createdAt(e.getCreatedAt())
                .resolvedAt(e.getResolvedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
