package com.lokesh.ecommerce.common.event;

import com.lokesh.ecommerce.common.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentProcessedEvent(
        String orderId,
        String paymentId,
        BigDecimal amount,
        PaymentStatus status,
        String transactionRef,
        LocalDateTime timestamp
) {
}
