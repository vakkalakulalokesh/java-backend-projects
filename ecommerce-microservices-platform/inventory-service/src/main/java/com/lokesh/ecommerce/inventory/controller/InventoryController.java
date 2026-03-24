package com.lokesh.ecommerce.inventory.controller;

import com.lokesh.ecommerce.inventory.dto.InventoryResponse;
import com.lokesh.ecommerce.inventory.dto.ReservationResponse;
import com.lokesh.ecommerce.inventory.dto.StockUpdateRequest;
import com.lokesh.ecommerce.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    @Operation(summary = "Get inventory by product")
    public InventoryResponse get(@PathVariable String productId) {
        return inventoryService.getInventory(productId);
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Low stock items")
    public List<InventoryResponse> lowStock() {
        return inventoryService.getLowStockItems();
    }

    @PostMapping("/restock")
    @Operation(summary = "Adjust stock")
    public InventoryResponse restock(@Valid @RequestBody StockUpdateRequest request) {
        return inventoryService.updateStock(request);
    }

    @GetMapping("/reservations/{orderId}")
    @Operation(summary = "Reservations for order")
    public List<ReservationResponse> reservations(@PathVariable String orderId) {
        return inventoryService.listReservations(orderId);
    }
}
