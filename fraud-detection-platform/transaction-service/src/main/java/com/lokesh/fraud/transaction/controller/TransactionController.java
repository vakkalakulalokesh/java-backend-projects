package com.lokesh.fraud.transaction.controller;

import com.lokesh.fraud.transaction.dto.TransactionRequest;
import com.lokesh.fraud.transaction.dto.TransactionResponse;
import com.lokesh.fraud.transaction.dto.TransactionSearchCriteria;
import com.lokesh.fraud.transaction.dto.TransactionStatsResponse;
import com.lokesh.fraud.transaction.dto.TransactionStatusUpdateRequest;
import com.lokesh.fraud.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction ingestion and queries")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create and submit a transaction for fraud screening")
    public TransactionResponse create(@Valid @RequestBody TransactionRequest request) {
        return transactionService.createTransaction(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by id")
    public TransactionResponse get(@PathVariable("id") String transactionId) {
        return transactionService.getTransaction(transactionId);
    }

    @GetMapping("/search")
    @Operation(summary = "Search transactions with optional filters")
    public List<TransactionResponse> search(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) com.lokesh.fraud.common.enums.TransactionStatus status,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        TransactionSearchCriteria criteria = TransactionSearchCriteria.builder()
                .accountId(accountId)
                .merchantId(merchantId)
                .status(status)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .startDate(startDate)
                .endDate(endDate)
                .build();
        return transactionService.searchTransactions(criteria);
    }

    @GetMapping("/stats")
    @Operation(summary = "Aggregate transaction statistics")
    public TransactionStatsResponse stats() {
        return transactionService.getTransactionStats();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update transaction status (manual override)")
    public TransactionResponse updateStatus(
            @PathVariable("id") String transactionId,
            @Valid @RequestBody TransactionStatusUpdateRequest body
    ) {
        return transactionService.updateTransactionStatus(transactionId, body.getStatus());
    }
}
