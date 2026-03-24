package com.lokesh.ecommerce.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.ecommerce.common.enums.OrderStatus;
import com.lokesh.ecommerce.common.enums.PaymentStatus;
import com.lokesh.ecommerce.common.event.PaymentFailedEvent;
import com.lokesh.ecommerce.common.event.PaymentProcessedEvent;
import com.lokesh.ecommerce.notification.entity.DeliveryStatus;
import com.lokesh.ecommerce.notification.entity.NotificationChannel;
import com.lokesh.ecommerce.notification.entity.NotificationLog;
import com.lokesh.ecommerce.notification.entity.NotificationType;
import com.lokesh.ecommerce.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processStatusPayload(String payload) throws Exception {
        JsonNode n = objectMapper.readTree(payload);
        if (!n.hasNonNull("orderId") || !n.hasNonNull("status")) {
            return;
        }
        if (!n.get("status").isTextual()) {
            return;
        }
        OrderStatus status = OrderStatus.valueOf(n.get("status").asText());
        String customerId = n.path("customerId").asText("guest");
        String reason = n.hasNonNull("reason") ? n.get("reason").asText() : null;
        dispatchForOrderStatus(n.get("orderId").asText(), customerId, status, reason);
    }

    @Transactional
    public void processPaymentPayload(String payload) throws Exception {
        JsonNode n = objectMapper.readTree(payload);
        if (n.hasNonNull("paymentId")) {
            PaymentProcessedEvent event = objectMapper.treeToValue(n, PaymentProcessedEvent.class);
            processPaymentProcessed(event);
        } else if (n.hasNonNull("reason")) {
            PaymentFailedEvent event = objectMapper.treeToValue(n, PaymentFailedEvent.class);
            processPaymentFailed(event);
        }
    }

    private void dispatchForOrderStatus(String orderId, String customerId, OrderStatus status, String reason) {
        switch (status) {
            case CREATED -> persistAndSend(
                    orderId, customerId, NotificationType.ORDER_CREATED,
                    "Order received", "Your order " + orderId + " was created."
            );
            case PAYMENT_PENDING -> persistAndSend(
                    orderId, customerId, NotificationType.ORDER_CREATED,
                    "Payment pending", "We are processing payment for " + orderId + "."
            );
            case CONFIRMED -> processOrderCompletion(orderId, customerId);
            case CANCELLED, REFUNDED, PAYMENT_FAILED, INVENTORY_FAILED -> persistAndSend(
                    orderId, customerId, NotificationType.ORDER_CANCELLED,
                    "Order update", "Order " + orderId + " status: " + status + (reason != null ? (". " + reason) : "")
            );
            default -> persistAndSend(
                    orderId, customerId, NotificationType.ORDER_CONFIRMED,
                    "Order status", "Order " + orderId + " is now " + status
            );
        }
    }

    @Transactional
    public void processPaymentProcessed(PaymentProcessedEvent event) {
        if (event.status() != PaymentStatus.COMPLETED) {
            processPaymentFailed(new PaymentFailedEvent(event.orderId(), "Payment not completed", event.timestamp()));
            return;
        }
        persistAndSend(
                event.orderId(),
                "guest",
                NotificationType.PAYMENT_SUCCESS,
                "Payment confirmed",
                "Payment " + event.paymentId() + " completed for order " + event.orderId()
        );
    }

    @Transactional
    public void processPaymentFailed(PaymentFailedEvent event) {
        persistAndSend(
                event.orderId(),
                "guest",
                NotificationType.PAYMENT_FAILED,
                "Payment failed",
                event.reason()
        );
    }

    @Transactional
    public void processOrderCompletion(String orderId, String customerId) {
        persistAndSend(
                orderId,
                customerId,
                NotificationType.ORDER_CONFIRMED,
                "Order confirmed",
                "Your order " + orderId + " is confirmed and being prepared."
        );
    }

    private void persistAndSend(String orderId, String customerId, NotificationType type, String subject, String body) {
        NotificationLog logEntry = NotificationLog.builder()
                .orderId(orderId)
                .customerId(customerId)
                .channel(NotificationChannel.EMAIL)
                .type(type)
                .subject(subject)
                .body(body)
                .status(DeliveryStatus.SENT)
                .sentAt(Instant.now())
                .build();
        notificationLogRepository.save(logEntry);
        emailService.send(customerId + "@customers.example", subject, body);
        smsService.send("+10000000000", subject + ": " + body);
    }
}
