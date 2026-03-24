package com.lokesh.ecommerce.order.saga;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SagaStep {
    PAYMENT("orders.payment-request", "orders.payment-response"),
    INVENTORY("orders.inventory-request", "orders.inventory-response");

    private final String requestTopic;
    private final String responseTopic;
}
