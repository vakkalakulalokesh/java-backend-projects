package com.lokesh.ecommerce.payment.repository;

import com.lokesh.ecommerce.common.enums.PaymentStatus;
import com.lokesh.ecommerce.payment.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByPaymentId(String paymentId);

    List<PaymentEntity> findByOrderId(String orderId);

    Optional<PaymentEntity> findByIdempotencyKey(String idempotencyKey);

    List<PaymentEntity> findByStatus(PaymentStatus status);
}
