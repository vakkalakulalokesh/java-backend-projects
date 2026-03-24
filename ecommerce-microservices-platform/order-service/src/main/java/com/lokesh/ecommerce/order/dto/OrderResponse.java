package com.lokesh.ecommerce.order.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.ecommerce.common.enums.OrderStatus;
import com.lokesh.ecommerce.order.entity.OrderEntity;
import com.lokesh.ecommerce.order.entity.OrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderId,
        String customerId,
        List<OrderItem> items,
        BigDecimal totalAmount,
        String shippingAddress,
        OrderStatus status,
        String paymentId,
        String reservationId,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    public static OrderResponse fromEntity(OrderEntity e) {
        try {
            List<OrderItem> items = MAPPER.readValue(e.getItemsJson(), new TypeReference<>() {
            });
            return new OrderResponse(
                    e.getId(),
                    e.getOrderId(),
                    e.getCustomerId(),
                    items,
                    e.getTotalAmount(),
                    e.getShippingAddress(),
                    e.getStatus(),
                    e.getPaymentId(),
                    e.getReservationId(),
                    e.getFailureReason(),
                    e.getCreatedAt(),
                    e.getUpdatedAt()
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid order items JSON", ex);
        }
    }
}
