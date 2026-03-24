package com.lokesh.fraud.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lokesh.fraud.common.enums.RiskLevel;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FraudAlertEvent(
        String alertId,
        String transactionId,
        String accountId,
        double riskScore,
        RiskLevel riskLevel,
        List<String> triggeredRules,
        String recommendation,
        LocalDateTime timestamp
) {
}
