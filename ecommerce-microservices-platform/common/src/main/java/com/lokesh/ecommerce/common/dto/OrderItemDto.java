package com.lokesh.ecommerce.common.dto;

import java.math.BigDecimal;

public record OrderItemDto(
        String productId,
        String productName,
        int quantity,
        BigDecimal unitPrice
) {
}
