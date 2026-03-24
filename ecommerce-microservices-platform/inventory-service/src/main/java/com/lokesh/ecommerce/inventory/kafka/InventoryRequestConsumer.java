package com.lokesh.ecommerce.inventory.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.ecommerce.inventory.entity.ReservationEntity;
import com.lokesh.ecommerce.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryRequestConsumer {

    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;
    private final InventoryEventProducer inventoryEventProducer;

    @KafkaListener(topics = InventoryKafkaTopics.ORDERS_INVENTORY_REQUEST, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onMessage(String payload) {
        try {
            InventoryRequestPayload msg = objectMapper.readValue(payload, InventoryRequestPayload.class);
            List<InventoryService.Line> lines = msg.getLines().stream()
                    .map(l -> new InventoryService.Line(l.getProductId(), l.getProductName(), l.getQuantity()))
                    .toList();
            InventoryService.ReservationBatch batch = inventoryService.reserveAllLines(msg.getOrderId(), lines);
            inventoryService.confirmReservationForOrder(msg.getOrderId());
            inventoryEventProducer.publishReserved(msg.getOrderId(), batch.groupId(), batch.reservations());
        } catch (InventoryService.InsufficientStockException ex) {
            inventoryEventProducer.publishInsufficient(extractOrderId(payload), List.of(ex.getProductId()));
        } catch (Exception e) {
            log.error("Inventory request failed: {}", e.getMessage(), e);
            try {
                InventoryRequestPayload msg = objectMapper.readValue(payload, InventoryRequestPayload.class);
                inventoryEventProducer.publishInsufficient(msg.getOrderId(), List.of("unknown"));
            } catch (Exception ignored) {
                log.warn("Could not publish inventory failure event");
            }
        }
    }

    private String extractOrderId(String payload) {
        try {
            return objectMapper.readTree(payload).get("orderId").asText();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
