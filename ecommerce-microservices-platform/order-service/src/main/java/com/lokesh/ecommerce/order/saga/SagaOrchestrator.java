package com.lokesh.ecommerce.order.saga;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.ecommerce.common.dto.OrderItemDto;
import com.lokesh.ecommerce.common.enums.OrderStatus;
import com.lokesh.ecommerce.common.enums.PaymentStatus;
import com.lokesh.ecommerce.common.enums.SagaStatus;
import com.lokesh.ecommerce.common.event.InventoryInsufficientEvent;
import com.lokesh.ecommerce.common.event.InventoryReservedEvent;
import com.lokesh.ecommerce.common.event.PaymentFailedEvent;
import com.lokesh.ecommerce.common.event.PaymentProcessedEvent;
import com.lokesh.ecommerce.order.client.PaymentCompensationClient;
import com.lokesh.ecommerce.order.dto.OrderStatusUpdate;
import com.lokesh.ecommerce.order.entity.OrderEntity;
import com.lokesh.ecommerce.order.entity.OrderItem;
import com.lokesh.ecommerce.order.entity.SagaState;
import com.lokesh.ecommerce.order.kafka.InventoryRequestMessage;
import com.lokesh.ecommerce.order.kafka.OrderEventProducer;
import com.lokesh.ecommerce.order.kafka.PaymentRequestMessage;
import com.lokesh.ecommerce.order.repository.OrderRepository;
import com.lokesh.ecommerce.order.repository.SagaStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestrator {

    private final SagaStateRepository sagaStateRepository;
    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;
    private final ObjectMapper objectMapper;
    private final SagaRedisSupport sagaRedisSupport;
    private final PaymentCompensationClient paymentCompensationClient;

    @Transactional
    public void startSaga(OrderEntity order) {
        String sagaId = UUID.randomUUID().toString();
        Map<String, Object> payload = basePayload(order);
        SagaState state = SagaState.builder()
                .sagaId(sagaId)
                .orderId(order.getOrderId())
                .currentStep(SagaStatus.STARTED)
                .payload(writeJson(payload))
                .failed(false)
                .build();
        sagaStateRepository.save(state);
        sagaRedisSupport.put(state);

        state.setCurrentStep(SagaStatus.PAYMENT_PENDING);
        sagaStateRepository.save(state);
        sagaRedisSupport.put(state);

        orderEventProducer.publishStatusUpdate(
                OrderStatusUpdate.builder()
                        .orderId(order.getOrderId())
                        .status(OrderStatus.CREATED)
                        .build()
        );

        order.setStatus(OrderStatus.PAYMENT_PENDING);
        orderRepository.save(order);

        List<OrderItemDto> itemDtos = toDtos(readItems(order));
        orderEventProducer.publishOrderCreated(
                orderEventProducer.buildOrderCreated(
                        order.getOrderId(),
                        order.getCustomerId(),
                        itemDtos,
                        order.getTotalAmount(),
                        order.getShippingAddress()
                )
        );

        orderEventProducer.publishPaymentRequest(
                PaymentRequestMessage.builder()
                        .sagaId(sagaId)
                        .orderId(order.getOrderId())
                        .amount(order.getTotalAmount())
                        .currency("USD")
                        .idempotencyKey(order.getOrderId())
                        .build()
        );

        orderEventProducer.publishStatusUpdate(
                OrderStatusUpdate.builder()
                        .orderId(order.getOrderId())
                        .status(OrderStatus.PAYMENT_PENDING)
                        .build()
        );
    }

    @Transactional
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        if (event.status() != PaymentStatus.COMPLETED) {
            handlePaymentFailed(new PaymentFailedEvent(event.orderId(), "Payment not completed", event.timestamp()));
            return;
        }
        SagaState saga = findActiveForOrder(event.orderId()).orElse(null);
        if (saga == null || saga.getCurrentStep() != SagaStatus.PAYMENT_PENDING) {
            log.warn("No active PAYMENT_PENDING saga for order {}", event.orderId());
            return;
        }

        OrderEntity order = orderRepository.findByOrderId(event.orderId()).orElseThrow();
        order.setPaymentId(event.paymentId());
        order.setStatus(OrderStatus.PAYMENT_COMPLETED);
        orderRepository.save(order);

        saga.setCurrentStep(SagaStatus.PAYMENT_COMPLETED);
        saga.setCompensationData(writeJson(Map.of("paymentId", event.paymentId())));
        sagaStateRepository.save(saga);
        sagaRedisSupport.put(saga);

        saga.setCurrentStep(SagaStatus.INVENTORY_PENDING);
        sagaStateRepository.save(saga);
        sagaRedisSupport.put(saga);

        order.setStatus(OrderStatus.INVENTORY_RESERVING);
        orderRepository.save(order);

        List<OrderItem> items = readItems(order);
        List<InventoryRequestMessage.InventoryLine> lines = items.stream()
                .map(i -> InventoryRequestMessage.InventoryLine.builder()
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .quantity(i.getQuantity())
                        .build())
                .toList();

        orderEventProducer.publishInventoryRequest(
                InventoryRequestMessage.builder()
                        .sagaId(saga.getSagaId())
                        .orderId(order.getOrderId())
                        .lines(lines)
                        .build()
        );

        orderEventProducer.publishStatusUpdate(
                OrderStatusUpdate.builder()
                        .orderId(order.getOrderId())
                        .status(OrderStatus.INVENTORY_RESERVING)
                        .build()
        );
    }

    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        SagaState saga = findActiveForOrder(event.orderId()).orElse(null);
        if (saga == null) {
            log.warn("No saga for failed payment order {}", event.orderId());
            return;
        }
        saga.setCurrentStep(SagaStatus.PAYMENT_FAILED);
        saga.setFailed(true);
        saga.setFailureReason(event.reason());
        saga.setCompletedAt(java.time.Instant.now());
        sagaStateRepository.save(saga);
        sagaRedisSupport.put(saga);

        OrderEntity order = orderRepository.findByOrderId(event.orderId()).orElseThrow();
        order.setStatus(OrderStatus.PAYMENT_FAILED);
        order.setFailureReason(event.reason());
        orderRepository.save(order);

        compensate(saga.getSagaId(), "Payment failed: " + event.reason());
    }

    @Transactional
    public void handleInventoryReserved(InventoryReservedEvent event) {
        SagaState saga = findActiveForOrder(event.orderId()).orElse(null);
        if (saga == null || saga.getCurrentStep() != SagaStatus.INVENTORY_PENDING) {
            log.warn("No INVENTORY_PENDING saga for order {}", event.orderId());
            return;
        }

        saga.setCurrentStep(SagaStatus.INVENTORY_RESERVED);
        sagaStateRepository.save(saga);
        sagaRedisSupport.put(saga);

        saga.setCurrentStep(SagaStatus.COMPLETED);
        saga.setCompletedAt(java.time.Instant.now());
        sagaStateRepository.save(saga);
        sagaRedisSupport.put(saga);

        OrderEntity order = orderRepository.findByOrderId(event.orderId()).orElseThrow();
        order.setReservationId(event.reservationId());
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        orderEventProducer.publishOrderCompleted(order.getOrderId(), OrderStatus.CONFIRMED);
        orderEventProducer.publishStatusUpdate(
                OrderStatusUpdate.builder()
                        .orderId(order.getOrderId())
                        .status(OrderStatus.CONFIRMED)
                        .build()
        );
    }

    @Transactional
    public void handleInventoryInsufficient(InventoryInsufficientEvent event) {
        SagaState saga = findActiveForOrder(event.orderId()).orElse(null);
        if (saga == null) {
            log.warn("No saga for inventory failure order {}", event.orderId());
            return;
        }
        saga.setCurrentStep(SagaStatus.INVENTORY_FAILED);
        saga.setFailed(true);
        saga.setFailureReason("Insufficient stock: " + String.join(",", event.failedItems()));
        sagaStateRepository.save(saga);
        sagaRedisSupport.put(saga);

        OrderEntity order = orderRepository.findByOrderId(event.orderId()).orElseThrow();
        order.setStatus(OrderStatus.INVENTORY_FAILED);
        order.setFailureReason(saga.getFailureReason());
        orderRepository.save(order);

        compensate(saga.getSagaId(), saga.getFailureReason());
    }

    @Transactional
    public void compensate(String sagaId, String reason) {
        SagaState saga = sagaStateRepository.findBySagaId(sagaId).orElse(null);
        if (saga == null) {
            return;
        }
        if (saga.getCurrentStep() == SagaStatus.COMPENSATING) {
            return;
        }

        saga.setCurrentStep(SagaStatus.COMPENSATING);
        sagaStateRepository.save(saga);
        sagaRedisSupport.put(saga);

        OrderEntity order = orderRepository.findByOrderId(saga.getOrderId()).orElseThrow();

        if (order.getPaymentId() != null && order.getStatus() != OrderStatus.REFUNDED) {
            paymentCompensationClient.refund(order.getPaymentId(), reason);
            order.setStatus(OrderStatus.REFUNDED);
        } else if (order.getStatus() != OrderStatus.CANCELLED && order.getStatus() != OrderStatus.REFUNDED) {
            order.setStatus(OrderStatus.CANCELLED);
        }

        order.setFailureReason(reason);
        orderRepository.save(order);

        saga.setCurrentStep(SagaStatus.FAILED);
        saga.setFailed(true);
        saga.setFailureReason(reason);
        saga.setCompletedAt(java.time.Instant.now());
        sagaStateRepository.save(saga);
        sagaRedisSupport.put(saga);

        orderEventProducer.publishOrderCancelled(order.getOrderId(), reason);
        orderEventProducer.publishStatusUpdate(
                OrderStatusUpdate.builder()
                        .orderId(order.getOrderId())
                        .status(order.getStatus())
                        .reason(reason)
                        .build()
        );
    }

    private java.util.Optional<SagaState> findActiveForOrder(String orderId) {
        return sagaStateRepository.findByOrderId(orderId).stream()
                .filter(s -> s.getCurrentStep() != SagaStatus.COMPLETED)
                .filter(s -> s.getCurrentStep() != SagaStatus.FAILED)
                .max(Comparator.comparing(SagaState::getStartedAt));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }

    private Map<String, Object> basePayload(OrderEntity order) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", order.getOrderId());
        map.put("customerId", order.getCustomerId());
        map.put("totalAmount", order.getTotalAmount());
        map.put("items", readItems(order));
        return map;
    }

    private List<OrderItem> readItems(OrderEntity order) {
        try {
            return objectMapper.readValue(order.getItemsJson(), new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private List<OrderItemDto> toDtos(List<OrderItem> items) {
        return items.stream()
                .map(i -> new OrderItemDto(i.getProductId(), i.getProductName(), i.getQuantity(), i.getUnitPrice()))
                .toList();
    }
}
