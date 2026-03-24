package com.lokesh.ecommerce.product.dto;

import com.lokesh.ecommerce.product.entity.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        String category,
        String brand,
        String imageUrl,
        boolean active,
        double rating,
        int reviewCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductResponse fromEntity(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getSku(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getCategory(),
                p.getBrand(),
                p.getImageUrl(),
                p.isActive(),
                p.getRating(),
                p.getReviewCount(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
