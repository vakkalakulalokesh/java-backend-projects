package com.lokesh.ecommerce.inventory.dto;

import com.lokesh.ecommerce.inventory.entity.ReservationEntity;
import com.lokesh.ecommerce.inventory.entity.ReservationStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        String reservationId,
        String orderId,
        String productId,
        int quantity,
        ReservationStatus status,
        Instant createdAt,
        LocalDateTime expiresAt,
        Instant updatedAt
) {
    public static ReservationResponse fromEntity(ReservationEntity e) {
        return new ReservationResponse(
                e.getId(),
                e.getReservationId(),
                e.getOrderId(),
                e.getProductId(),
                e.getQuantity(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getExpiresAt(),
                e.getUpdatedAt()
        );
    }
}
