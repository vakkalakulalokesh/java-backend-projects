package com.lokesh.fraud.alert.dto;

import com.lokesh.fraud.common.enums.AlertStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertUpdateRequest {

    private AlertStatus status;
    private String analystNotes;
    private String resolvedBy;
}
