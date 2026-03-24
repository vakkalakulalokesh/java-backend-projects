package com.lokesh.fraud.transaction.dto;

import com.lokesh.fraud.common.enums.TransactionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStatusUpdateRequest {

    @NotNull
    private TransactionStatus status;
}
