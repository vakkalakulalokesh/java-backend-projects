package com.lokesh.ecommerce.inventory.dto;

import com.lokesh.ecommerce.inventory.entity.InventoryEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryResponse(
        UUID id,
        String productId,
        String productName,
        int availableQuantity,
        int reservedQuantity,
        String warehouseId,
        int reorderLevel,
        LocalDateTime lastRestockedAt,
        Instant updatedAt,
        Long version
) {
    public static InventoryResponse fromEntity(InventoryEntity e) {
        return new InventoryResponse(
                e.getId(),
                e.getProductId(),
                e.getProductName(),
                e.getAvailableQuantity(),
                e.getReservedQuantity(),
                e.getWarehouseId(),
                e.getReorderLevel(),
                e.getLastRestockedAt(),
                e.getUpdatedAt(),
                e.getVersion()
        );
    }
}
