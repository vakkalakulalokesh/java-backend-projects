package com.lokesh.ecommerce.order.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestMessage {

    private String sagaId;
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String idempotencyKey;
}
