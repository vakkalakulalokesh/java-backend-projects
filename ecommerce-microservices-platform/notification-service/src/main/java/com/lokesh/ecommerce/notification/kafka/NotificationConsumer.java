package com.lokesh.ecommerce.notification.kafka;

import com.lokesh.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = NotificationTopics.ORDERS_STATUS_UPDATE, groupId = "${spring.kafka.consumer.group-id}")
    public void onStatus(String payload) {
        try {
            notificationService.processStatusPayload(payload);
        } catch (Exception e) {
            log.warn("Status notification skipped: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = NotificationTopics.ORDERS_PAYMENT_RESPONSE, groupId = "${spring.kafka.consumer.group-id}")
    public void onPayment(String payload) {
        try {
            notificationService.processPaymentPayload(payload);
        } catch (Exception e) {
            log.warn("Payment notification skipped: {}", e.getMessage());
        }
    }
}
