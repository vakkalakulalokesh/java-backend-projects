package com.lokesh.ecommerce.inventory.service;

import com.lokesh.ecommerce.inventory.entity.ReservationStatus;
import com.lokesh.ecommerce.inventory.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationCleanupService {

    private final ReservationRepository reservationRepository;
    private final InventoryService inventoryService;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpired() {
        var expired = reservationRepository.findByExpiresAtBeforeAndStatus(LocalDateTime.now(), ReservationStatus.RESERVED);
        expired.forEach(r -> {
            try {
                inventoryService.releaseReservation(r.getReservationId());
            } catch (Exception e) {
                log.warn("Cleanup release failed for {}: {}", r.getReservationId(), e.getMessage());
            }
        });
    }
}
