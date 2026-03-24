package com.lokesh.fraud.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lokesh.fraud.common.enums.RiskLevel;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RiskAssessment(
        String transactionId,
        double riskScore,
        RiskLevel riskLevel,
        List<RuleResult> triggeredRules,
        String recommendation,
        LocalDateTime assessedAt
) {
}
