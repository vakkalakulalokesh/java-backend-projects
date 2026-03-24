package com.lokesh.fraud.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionEvent(
        String eventId,
        String transactionId,
        String accountId,
        BigDecimal amount,
        String currency,
        String merchantId,
        String merchantCategory,
        String cardNumber,
        String sourceIp,
        String geoLocation,
        String channel,
        LocalDateTime timestamp
) {
}
