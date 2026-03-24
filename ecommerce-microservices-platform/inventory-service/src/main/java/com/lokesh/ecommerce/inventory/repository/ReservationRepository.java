package com.lokesh.ecommerce.inventory.repository;

import com.lokesh.ecommerce.inventory.entity.ReservationEntity;
import com.lokesh.ecommerce.inventory.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<ReservationEntity, UUID> {

    Optional<ReservationEntity> findByReservationId(String reservationId);

    List<ReservationEntity> findByOrderId(String orderId);

    List<ReservationEntity> findByStatus(ReservationStatus status);

    List<ReservationEntity> findByExpiresAtBeforeAndStatus(LocalDateTime now, ReservationStatus status);
}
