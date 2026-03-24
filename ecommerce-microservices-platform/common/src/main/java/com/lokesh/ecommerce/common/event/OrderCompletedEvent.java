package com.lokesh.ecommerce.common.event;

import com.lokesh.ecommerce.common.enums.OrderStatus;

import java.time.LocalDateTime;

public record OrderCompletedEvent(
        String orderId,
        OrderStatus status,
        LocalDateTime timestamp
) {
}
