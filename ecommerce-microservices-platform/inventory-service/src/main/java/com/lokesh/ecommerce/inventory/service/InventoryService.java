package com.lokesh.ecommerce.inventory.service;

import com.lokesh.ecommerce.inventory.dto.InventoryResponse;
import com.lokesh.ecommerce.inventory.dto.ReservationResponse;
import com.lokesh.ecommerce.inventory.dto.StockUpdateRequest;
import com.lokesh.ecommerce.inventory.entity.InventoryEntity;
import com.lokesh.ecommerce.inventory.entity.ReservationEntity;
import com.lokesh.ecommerce.inventory.entity.ReservationStatus;
import com.lokesh.ecommerce.inventory.repository.InventoryRepository;
import com.lokesh.ecommerce.inventory.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final int MAX_RETRY = 4;
    private static final String DEFAULT_WAREHOUSE = "WH-01";

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public boolean checkStock(String productId, int quantity) {
        return inventoryRepository.findByProductId(productId)
                .map(i -> i.getAvailableQuantity() >= quantity)
                .orElse(false);
    }

    @Transactional
    public ReservationEntity reserveStock(String orderId, String groupId, String productId, String productName, int quantity) {
        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            try {
                return doReserve(orderId, groupId, productId, productName, quantity);
            } catch (ObjectOptimisticLockingFailureException ex) {
                if (attempt == MAX_RETRY - 1) {
                    throw ex;
                }
            }
        }
        throw new IllegalStateException("Reserve failed after retries");
    }

    private ReservationEntity doReserve(String orderId, String groupId, String productId, String productName, int quantity) {
        InventoryEntity inv = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InsufficientStockException(productId, "Unknown product"));
        if (inv.getAvailableQuantity() < quantity) {
            throw new InsufficientStockException(productId, "Insufficient stock");
        }
        inv.setAvailableQuantity(inv.getAvailableQuantity() - quantity);
        inv.setReservedQuantity(inv.getReservedQuantity() + quantity);
        inventoryRepository.save(inv);

        String resId = groupId + "-" + productId;
        ReservationEntity res = ReservationEntity.builder()
                .reservationId(resId)
                .orderId(orderId)
                .productId(productId)
                .quantity(quantity)
                .status(ReservationStatus.RESERVED)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        return reservationRepository.save(res);
    }

    @Transactional
    public void releaseReservation(String reservationId) {
        ReservationEntity res = reservationRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
        if (res.getStatus() != ReservationStatus.RESERVED) {
            return;
        }
        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            try {
                InventoryEntity inv = inventoryRepository.findByProductId(res.getProductId()).orElseThrow();
                inv.setAvailableQuantity(inv.getAvailableQuantity() + res.getQuantity());
                inv.setReservedQuantity(Math.max(0, inv.getReservedQuantity() - res.getQuantity()));
                inventoryRepository.save(inv);
                res.setStatus(ReservationStatus.RELEASED);
                reservationRepository.save(res);
                return;
            } catch (ObjectOptimisticLockingFailureException ex) {
                if (attempt == MAX_RETRY - 1) {
                    throw ex;
                }
            }
        }
    }

    @Transactional
    public void confirmReservationForOrder(String orderId) {
        List<ReservationEntity> list = reservationRepository.findByOrderId(orderId);
        for (ReservationEntity res : list) {
            if (res.getStatus() != ReservationStatus.RESERVED) {
                continue;
            }
            for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
                try {
                    InventoryEntity inv = inventoryRepository.findByProductId(res.getProductId()).orElseThrow();
                    inv.setReservedQuantity(Math.max(0, inv.getReservedQuantity() - res.getQuantity()));
                    inventoryRepository.save(inv);
                    res.setStatus(ReservationStatus.CONFIRMED);
                    reservationRepository.save(res);
                    break;
                } catch (ObjectOptimisticLockingFailureException ex) {
                    if (attempt == MAX_RETRY - 1) {
                        throw ex;
                    }
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(String productId) {
        return inventoryRepository.findByProductId(productId)
                .map(InventoryResponse::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found: " + productId));
    }

    @Transactional
    public InventoryResponse updateStock(StockUpdateRequest request) {
        InventoryEntity inv = inventoryRepository.findByProductId(request.getProductId())
                .orElseGet(() -> InventoryEntity.builder()
                        .productId(request.getProductId())
                        .productName(request.getProductId())
                        .availableQuantity(0)
                        .reservedQuantity(0)
                        .warehouseId(DEFAULT_WAREHOUSE)
                        .reorderLevel(10)
                        .build());
        inv.setAvailableQuantity(inv.getAvailableQuantity() + request.getQuantity());
        if (request.getQuantity() > 0) {
            inv.setLastRestockedAt(LocalDateTime.now());
        }
        return InventoryResponse.fromEntity(inventoryRepository.save(inv));
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStockItems() {
        return inventoryRepository.findAll().stream()
                .filter(i -> i.getAvailableQuantity() <= i.getReorderLevel())
                .map(InventoryResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listReservations(String orderId) {
        return reservationRepository.findByOrderId(orderId).stream()
                .map(ReservationResponse::fromEntity)
                .toList();
    }

    @Transactional
    public ReservationBatch reserveAllLines(String orderId, List<Line> lines) {
        String groupId = UUID.randomUUID().toString();
        List<ReservationEntity> created = new ArrayList<>();
        for (Line line : lines) {
            created.add(reserveStock(orderId, groupId, line.productId(), line.productName(), line.quantity()));
        }
        return new ReservationBatch(groupId, created);
    }

    public record ReservationBatch(String groupId, List<ReservationEntity> reservations) {
    }

    @Transactional
    public void releaseAllForOrder(String orderId) {
        reservationRepository.findByOrderId(orderId).stream()
                .filter(r -> r.getStatus() == ReservationStatus.RESERVED)
                .map(ReservationEntity::getReservationId)
                .forEach(this::releaseReservation);
    }

    public record Line(String productId, String productName, int quantity) {
    }

    public static final class InsufficientStockException extends RuntimeException {
        private final String productId;

        public InsufficientStockException(String productId, String message) {
            super(message);
            this.productId = productId;
        }

        public String getProductId() {
            return productId;
        }
    }
}
