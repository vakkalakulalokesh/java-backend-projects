package com.lokesh.ecommerce.common.dto;

public record ReservedItemDto(
        String productId,
        int quantity,
        String warehouseId
) {
}
