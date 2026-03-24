package com.lokesh.ecommerce.order.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.ecommerce.common.event.InventoryInsufficientEvent;
import com.lokesh.ecommerce.common.event.InventoryReservedEvent;
import com.lokesh.ecommerce.common.event.PaymentFailedEvent;
import com.lokesh.ecommerce.common.event.PaymentProcessedEvent;
import com.lokesh.ecommerce.order.config.KafkaTopics;
import com.lokesh.ecommerce.order.saga.SagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaResponseConsumer {

    private final ObjectMapper objectMapper;
    private final SagaOrchestrator sagaOrchestrator;

    @KafkaListener(topics = KafkaTopics.ORDERS_PAYMENT_RESPONSE, groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentResponse(String payload) {
        try {
            JsonNode n = objectMapper.readTree(payload);
            if (n.hasNonNull("paymentId") && n.has("status")) {
                PaymentProcessedEvent event = objectMapper.treeToValue(n, PaymentProcessedEvent.class);
                sagaOrchestrator.handlePaymentProcessed(event);
            } else if (n.has("reason")) {
                PaymentFailedEvent event = new PaymentFailedEvent(
                        n.get("orderId").asText(),
                        n.get("reason").asText(),
                        parseTime(n.get("timestamp"))
                );
                sagaOrchestrator.handlePaymentFailed(event);
            }
        } catch (Exception e) {
            log.error("Payment response handling failed: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = KafkaTopics.ORDERS_INVENTORY_RESPONSE, groupId = "${spring.kafka.consumer.group-id}")
    public void onInventoryResponse(String payload) {
        try {
            JsonNode n = objectMapper.readTree(payload);
            if (n.hasNonNull("reservationId")) {
                InventoryReservedEvent event = objectMapper.treeToValue(n, InventoryReservedEvent.class);
                sagaOrchestrator.handleInventoryReserved(event);
            } else if (n.has("failedItems")) {
                List<String> failed = new ArrayList<>();
                Iterator<JsonNode> it = n.get("failedItems").elements();
                while (it.hasNext()) {
                    failed.add(it.next().asText());
                }
                InventoryInsufficientEvent event = new InventoryInsufficientEvent(
                        n.get("orderId").asText(),
                        failed,
                        parseTime(n.get("timestamp"))
                );
                sagaOrchestrator.handleInventoryInsufficient(event);
            }
        } catch (Exception e) {
            log.error("Inventory response handling failed: {}", e.getMessage(), e);
        }
    }

    private static LocalDateTime parseTime(JsonNode node) {
        if (node == null || node.isNull()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(node.asText());
        } catch (DateTimeParseException e) {
            return LocalDateTime.now();
        }
    }
}
