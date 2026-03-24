package com.lokesh.fraud.transaction.dto;

import com.lokesh.fraud.common.enums.TransactionStatus;
import com.lokesh.fraud.transaction.entity.TransactionEntity;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TransactionResponse(
        String transactionId,
        String accountId,
        BigDecimal amount,
        TransactionStatus status,
        Double riskScore,
        LocalDateTime createdAt
) {

    public static TransactionResponse fromEntity(TransactionEntity entity) {
        return TransactionResponse.builder()
                .transactionId(entity.getTransactionId())
                .accountId(entity.getAccountId())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .riskScore(entity.getRiskScore())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
