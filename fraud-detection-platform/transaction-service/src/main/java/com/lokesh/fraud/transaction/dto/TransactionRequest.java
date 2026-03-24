package com.lokesh.fraud.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    @NotBlank
    @Size(max = 64)
    private String accountId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    @Size(max = 8)
    private String currency;

    @NotBlank
    @Size(max = 64)
    private String merchantId;

    @NotBlank
    @Size(max = 128)
    private String merchantCategory;

    @NotBlank
    @Size(min = 4, max = 19)
    private String cardNumber;

    @NotBlank
    @Size(max = 64)
    private String sourceIp;

    @NotBlank
    @Size(max = 128)
    private String geoLocation;

    @NotBlank
    @Size(max = 32)
    private String channel;
}
