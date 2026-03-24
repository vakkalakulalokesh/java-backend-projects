package com.lokesh.ecommerce.notification.kafka;

public final class NotificationTopics {

    public static final String ORDERS_STATUS_UPDATE = "orders.status-update";
    public static final String ORDERS_PAYMENT_RESPONSE = "orders.payment-response";

    private NotificationTopics() {
    }
}
