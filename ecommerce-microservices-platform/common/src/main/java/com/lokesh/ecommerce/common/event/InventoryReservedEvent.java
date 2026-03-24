package com.lokesh.ecommerce.common.event;

import com.lokesh.ecommerce.common.dto.ReservedItemDto;

import java.time.LocalDateTime;
import java.util.List;

public record InventoryReservedEvent(
        String orderId,
        String reservationId,
        List<ReservedItemDto> items,
        LocalDateTime timestamp
) {
}
