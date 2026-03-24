package com.lokesh.ecommerce.order.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryRequestMessage {

    private String sagaId;
    private String orderId;
    private List<InventoryLine> lines;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InventoryLine {
        private String productId;
        private String productName;
        private int quantity;
    }
}
