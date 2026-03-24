package com.lokesh.ecommerce.order.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.ecommerce.common.dto.OrderItemDto;
import com.lokesh.ecommerce.common.event.OrderCancelledEvent;
import com.lokesh.ecommerce.common.event.OrderCompletedEvent;
import com.lokesh.ecommerce.common.event.OrderCreatedEvent;
import com.lokesh.ecommerce.common.enums.OrderStatus;
import com.lokesh.ecommerce.order.config.KafkaTopics;
import com.lokesh.ecommerce.order.dto.OrderStatusUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishOrderCreated(OrderCreatedEvent event) {
        send(KafkaTopics.ORDERS_CREATED, event.orderId(), event);
    }

    public void publishPaymentRequest(PaymentRequestMessage message) {
        send(KafkaTopics.ORDERS_PAYMENT_REQUEST, message.getOrderId(), message);
    }

    public void publishInventoryRequest(InventoryRequestMessage message) {
        send(KafkaTopics.ORDERS_INVENTORY_REQUEST, message.getOrderId(), message);
    }

    public void publishStatusUpdate(OrderStatusUpdate update) {
        send(KafkaTopics.ORDERS_STATUS_UPDATE, update.getOrderId(), update);
    }

    public void publishOrderCompleted(String orderId, OrderStatus status) {
        OrderCompletedEvent event = new OrderCompletedEvent(orderId, status, LocalDateTime.now());
        send(KafkaTopics.ORDERS_STATUS_UPDATE, orderId, event);
    }

    public void publishOrderCancelled(String orderId, String reason) {
        OrderCancelledEvent event = new OrderCancelledEvent(orderId, reason, LocalDateTime.now());
        send(KafkaTopics.ORDERS_STATUS_UPDATE, orderId, event);
    }

    private void send(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(payload));
        } catch (IOException e) {
            throw new IllegalStateException("Kafka publish failed", e);
        }
    }

    public OrderCreatedEvent buildOrderCreated(
            String orderId,
            String customerId,
            List<OrderItemDto> items,
            BigDecimal total,
            String shippingAddress
    ) {
        return new OrderCreatedEvent(orderId, customerId, items, total, shippingAddress, LocalDateTime.now());
    }
}
