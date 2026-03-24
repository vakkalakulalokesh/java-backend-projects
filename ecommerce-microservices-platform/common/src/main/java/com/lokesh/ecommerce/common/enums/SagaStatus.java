package com.lokesh.ecommerce.common.enums;

public enum SagaStatus {
    STARTED,
    PAYMENT_PENDING,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    INVENTORY_PENDING,
    INVENTORY_RESERVED,
    INVENTORY_FAILED,
    COMPENSATING,
    COMPLETED,
    FAILED
}
