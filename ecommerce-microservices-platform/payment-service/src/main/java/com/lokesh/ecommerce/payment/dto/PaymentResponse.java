package com.lokesh.ecommerce.payment.dto;

import com.lokesh.ecommerce.common.enums.PaymentStatus;
import com.lokesh.ecommerce.payment.entity.PaymentEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String paymentId,
        String orderId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String transactionRef,
        String gatewayResponse,
        String idempotencyKey,
        int retryCount,
        Instant createdAt,
        Instant processedAt,
        Instant updatedAt
) {
    public static PaymentResponse fromEntity(PaymentEntity e) {
        return new PaymentResponse(
                e.getId(),
                e.getPaymentId(),
                e.getOrderId(),
                e.getAmount(),
                e.getCurrency(),
                e.getStatus(),
                e.getTransactionRef(),
                e.getGatewayResponse(),
                e.getIdempotencyKey(),
                e.getRetryCount(),
                e.getCreatedAt(),
                e.getProcessedAt(),
                e.getUpdatedAt()
        );
    }
}
