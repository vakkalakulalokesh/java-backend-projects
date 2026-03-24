package com.lokesh.ecommerce.order.controller;

import com.lokesh.ecommerce.order.dto.CreateOrderRequest;
import com.lokesh.ecommerce.order.dto.OrderResponse;
import com.lokesh.ecommerce.order.entity.SagaState;
import com.lokesh.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create order and start saga")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping("/stats")
    @Operation(summary = "Order counts by status")
    public Map<String, Long> stats() {
        return orderService.getOrderStats();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by business id")
    public OrderResponse get(@PathVariable String id) {
        return orderService.getOrder(id);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Orders for customer")
    public List<OrderResponse> byCustomer(@PathVariable String customerId) {
        return orderService.getOrdersByCustomer(customerId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel order")
    public OrderResponse cancel(@PathVariable String id) {
        return orderService.cancelOrder(id);
    }

    @GetMapping("/{id}/saga-status")
    @Operation(summary = "Saga states for order")
    public List<SagaState> sagaStatus(@PathVariable String id) {
        return orderService.getSagaStatusForOrder(id);
    }
}
