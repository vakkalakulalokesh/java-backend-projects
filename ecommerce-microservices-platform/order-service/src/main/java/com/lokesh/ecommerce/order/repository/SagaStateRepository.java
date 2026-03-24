package com.lokesh.ecommerce.order.repository;

import com.lokesh.ecommerce.order.entity.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SagaStateRepository extends JpaRepository<SagaState, UUID> {

    Optional<SagaState> findBySagaId(String sagaId);

    List<SagaState> findByOrderId(String orderId);
}
