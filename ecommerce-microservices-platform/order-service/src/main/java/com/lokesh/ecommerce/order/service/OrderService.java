package com.lokesh.ecommerce.order.service;

import com.lokesh.ecommerce.order.dto.CreateOrderRequest;
import com.lokesh.ecommerce.order.dto.OrderResponse;
import com.lokesh.ecommerce.order.entity.SagaState;
import com.lokesh.ecommerce.order.repository.SagaStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderCommandService orderCommandService;
    private final OrderQueryService orderQueryService;
    private final SagaStateRepository sagaStateRepository;

    public OrderResponse createOrder(CreateOrderRequest request) {
        return orderCommandService.createOrder(request);
    }

    public OrderResponse getOrder(String orderId) {
        return orderQueryService.getOrder(orderId);
    }

    public List<OrderResponse> getOrdersByCustomer(String customerId) {
        return orderQueryService.getOrdersByCustomer(customerId);
    }

    public OrderResponse cancelOrder(String orderId) {
        return orderCommandService.cancelOrder(orderId);
    }

    public Map<String, Long> getOrderStats() {
        return orderQueryService.getOrderStats();
    }

    @Transactional(readOnly = true)
    public List<SagaState> getSagaStatusForOrder(String orderId) {
        return sagaStateRepository.findByOrderId(orderId);
    }
}
