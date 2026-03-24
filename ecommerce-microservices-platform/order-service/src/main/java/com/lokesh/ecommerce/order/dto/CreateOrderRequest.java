package com.lokesh.ecommerce.order.dto;

import com.lokesh.ecommerce.common.dto.AddressDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotBlank
    private String customerId;

    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;

    @NotNull
    @Valid
    private AddressDto shippingAddress;
}
