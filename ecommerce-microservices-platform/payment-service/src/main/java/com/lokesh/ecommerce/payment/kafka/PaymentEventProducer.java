package com.lokesh.ecommerce.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.ecommerce.common.enums.PaymentStatus;
import com.lokesh.ecommerce.common.event.PaymentFailedEvent;
import com.lokesh.ecommerce.common.event.PaymentProcessedEvent;
import com.lokesh.ecommerce.payment.entity.PaymentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishProcessed(PaymentEntity entity) {
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                entity.getOrderId(),
                entity.getPaymentId(),
                entity.getAmount(),
                PaymentStatus.COMPLETED,
                entity.getTransactionRef(),
                LocalDateTime.now()
        );
        send(entity.getOrderId(), event);
    }

    public void publishFailed(String orderId, String reason) {
        PaymentFailedEvent event = new PaymentFailedEvent(orderId, reason, LocalDateTime.now());
        send(orderId, event);
    }

    private void send(String key, Object payload) {
        try {
            kafkaTemplate.send(PaymentKafkaTopics.ORDERS_PAYMENT_RESPONSE, key, objectMapper.writeValueAsString(payload));
        } catch (IOException e) {
            throw new IllegalStateException("Kafka publish failed", e);
        }
    }
}
