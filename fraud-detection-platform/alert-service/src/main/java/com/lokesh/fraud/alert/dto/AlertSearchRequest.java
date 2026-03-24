package com.lokesh.fraud.alert.dto;

import com.lokesh.fraud.common.enums.AlertStatus;
import com.lokesh.fraud.common.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertSearchRequest {

    private RiskLevel riskLevel;
    private AlertStatus status;
    private String accountId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
