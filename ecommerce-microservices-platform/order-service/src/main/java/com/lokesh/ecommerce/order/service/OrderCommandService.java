package com.lokesh.ecommerce.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.ecommerce.common.enums.OrderStatus;
import com.lokesh.ecommerce.order.dto.CreateOrderRequest;
import com.lokesh.ecommerce.order.dto.OrderResponse;
import com.lokesh.ecommerce.order.entity.OrderEntity;
import com.lokesh.ecommerce.order.entity.OrderItem;
import com.lokesh.ecommerce.order.repository.OrderRepository;
import com.lokesh.ecommerce.order.saga.SagaOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final SagaOrchestrator sagaOrchestrator;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        List<OrderItem> items = request.getItems().stream()
                .map(i -> {
                    BigDecimal subtotal = i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
                    return OrderItem.builder()
                            .productId(i.getProductId())
                            .productName(i.getProductName())
                            .quantity(i.getQuantity())
                            .unitPrice(i.getUnitPrice())
                            .subtotal(subtotal)
                            .build();
                })
                .toList();
        BigDecimal total = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String address = formatAddress(request);
        String itemsJson;
        try {
            itemsJson = objectMapper.writeValueAsString(items);
        } catch (Exception e) {
            throw new IllegalStateException("Order serialization failed", e);
        }

        OrderEntity entity = OrderEntity.builder()
                .orderId(orderId)
                .customerId(request.getCustomerId())
                .itemsJson(itemsJson)
                .totalAmount(total)
                .shippingAddress(address)
                .status(OrderStatus.CREATED)
                .build();
        orderRepository.save(entity);
        sagaOrchestrator.startSaga(entity);
        return OrderResponse.fromEntity(orderRepository.findByOrderId(orderId).orElseThrow());
    }

    @Transactional
    public OrderResponse cancelOrder(String orderId) {
        OrderEntity order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Order cannot be cancelled in status " + order.getStatus());
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setFailureReason("Cancelled by user or system");
        orderRepository.save(order);
        return OrderResponse.fromEntity(order);
    }

    private static String formatAddress(CreateOrderRequest request) {
        var a = request.getShippingAddress();
        return a.street() + ", " + a.city() + ", " + a.state() + " " + a.zipCode() + ", " + a.country();
    }
}
