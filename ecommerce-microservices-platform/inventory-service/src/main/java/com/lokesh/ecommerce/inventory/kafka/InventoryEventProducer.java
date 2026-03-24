package com.lokesh.ecommerce.inventory.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.ecommerce.common.dto.ReservedItemDto;
import com.lokesh.ecommerce.common.event.InventoryInsufficientEvent;
import com.lokesh.ecommerce.common.event.InventoryReservedEvent;
import com.lokesh.ecommerce.inventory.entity.ReservationEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InventoryEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishReserved(String orderId, String groupId, List<ReservationEntity> reservations) {
        List<ReservedItemDto> items = reservations.stream()
                .map(r -> new ReservedItemDto(r.getProductId(), r.getQuantity(), "WH-01"))
                .toList();
        InventoryReservedEvent event = new InventoryReservedEvent(orderId, groupId, items, LocalDateTime.now());
        send(orderId, event);
    }

    public void publishInsufficient(String orderId, List<String> failedItems) {
        InventoryInsufficientEvent event = new InventoryInsufficientEvent(orderId, failedItems, LocalDateTime.now());
        send(orderId, event);
    }

    private void send(String key, Object payload) {
        try {
            kafkaTemplate.send(InventoryKafkaTopics.ORDERS_INVENTORY_RESPONSE, key, objectMapper.writeValueAsString(payload));
        } catch (IOException e) {
            throw new IllegalStateException("Kafka publish failed", e);
        }
    }
}
