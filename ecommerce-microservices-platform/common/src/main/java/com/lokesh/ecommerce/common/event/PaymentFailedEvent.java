package com.lokesh.ecommerce.common.event;

import java.time.LocalDateTime;

public record PaymentFailedEvent(
        String orderId,
        String reason,
        LocalDateTime timestamp
) {
}
