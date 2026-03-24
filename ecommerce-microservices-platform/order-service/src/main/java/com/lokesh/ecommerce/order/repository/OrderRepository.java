package com.lokesh.ecommerce.order.repository;

import com.lokesh.ecommerce.common.enums.OrderStatus;
import com.lokesh.ecommerce.order.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByOrderId(String orderId);

    List<OrderEntity> findByCustomerId(String customerId);

    List<OrderEntity> findByStatus(OrderStatus status);

    long countByStatus(OrderStatus status);

    List<OrderEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
