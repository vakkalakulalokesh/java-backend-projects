package com.lokesh.ecommerce.common.event;

import java.time.LocalDateTime;
import java.util.List;

public record InventoryInsufficientEvent(
        String orderId,
        List<String> failedItems,
        LocalDateTime timestamp
) {
}
