package com.lokesh.fraud.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RuleResult(
        String ruleName,
        boolean triggered,
        double score,
        String reason
) {
}
