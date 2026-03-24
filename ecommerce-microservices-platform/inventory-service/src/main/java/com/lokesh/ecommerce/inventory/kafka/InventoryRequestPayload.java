package com.lokesh.ecommerce.inventory.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryRequestPayload {

    private String sagaId;
    private String orderId;
    private List<Line> lines;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Line {
        private String productId;
        private String productName;
        private int quantity;
    }
}
