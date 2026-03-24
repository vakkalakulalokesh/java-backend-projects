package com.lokesh.ecommerce.common.dto;

public record AddressDto(
        String street,
        String city,
        String state,
        String zipCode,
        String country
) {
}
