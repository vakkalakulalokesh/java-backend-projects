package com.lokesh.ecommerce.inventory.repository;

import com.lokesh.ecommerce.inventory.entity.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<InventoryEntity, UUID> {

    Optional<InventoryEntity> findByProductId(String productId);

    List<InventoryEntity> findByAvailableQuantityLessThanEqual(int threshold);

    List<InventoryEntity> findByWarehouseId(String warehouseId);
}
