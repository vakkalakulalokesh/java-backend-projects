package com.lokesh.ecommerce.order.service;

import com.lokesh.ecommerce.common.enums.OrderStatus;
import com.lokesh.ecommerce.order.dto.OrderResponse;
import com.lokesh.ecommerce.order.entity.OrderEntity;
import com.lokesh.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .map(OrderResponse::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getOrderStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            stats.put(s.name(), orderRepository.countByStatus(s));
        }
        return stats;
    }

    @Transactional(readOnly = true)
    public OrderEntity getOrderEntity(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }
}
