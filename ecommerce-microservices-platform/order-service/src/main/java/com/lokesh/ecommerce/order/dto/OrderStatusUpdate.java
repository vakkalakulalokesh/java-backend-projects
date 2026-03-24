package com.lokesh.ecommerce.order.dto;

import com.lokesh.ecommerce.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdate {

    private String orderId;
    private OrderStatus status;
    private String reason;
}
