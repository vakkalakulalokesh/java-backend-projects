package com.lokesh.ecommerce.payment.kafka;

public final class PaymentKafkaTopics {

    public static final String ORDERS_PAYMENT_REQUEST = "orders.payment-request";
    public static final String ORDERS_PAYMENT_RESPONSE = "orders.payment-response";

    private PaymentKafkaTopics() {
    }
}
