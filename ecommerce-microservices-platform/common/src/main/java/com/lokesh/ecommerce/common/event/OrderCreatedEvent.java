package com.lokesh.ecommerce.common.event;

import com.lokesh.ecommerce.common.dto.OrderItemDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedEvent(
        String orderId,
        String customerId,
        List<OrderItemDto> items,
        BigDecimal totalAmount,
        String shippingAddress,
        LocalDateTime timestamp
) {
}
