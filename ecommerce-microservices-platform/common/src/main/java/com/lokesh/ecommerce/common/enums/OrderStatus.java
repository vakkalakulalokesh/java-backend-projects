package com.lokesh.ecommerce.common.enums;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    INVENTORY_RESERVING,
    INVENTORY_RESERVED,
    INVENTORY_FAILED,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED
}
