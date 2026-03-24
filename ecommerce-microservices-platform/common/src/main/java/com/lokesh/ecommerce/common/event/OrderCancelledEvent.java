package com.lokesh.ecommerce.common.event;

import java.time.LocalDateTime;

public record OrderCancelledEvent(
        String orderId,
        String reason,
        LocalDateTime timestamp
) {
}
