package com.lokesh.ecommerce.order.config;

public final class KafkaTopics {

    public static final String ORDERS_CREATED = "orders.created";
    public static final String ORDERS_PAYMENT_REQUEST = "orders.payment-request";
    public static final String ORDERS_PAYMENT_RESPONSE = "orders.payment-response";
    public static final String ORDERS_INVENTORY_REQUEST = "orders.inventory-request";
    public static final String ORDERS_INVENTORY_RESPONSE = "orders.inventory-response";
    public static final String ORDERS_STATUS_UPDATE = "orders.status-update";

    private KafkaTopics() {
    }
}
