package com.lokesh.fraud.transaction.service;

import com.lokesh.fraud.common.dto.TransactionEvent;
import com.lokesh.fraud.common.enums.TransactionStatus;
import com.lokesh.fraud.common.exception.FraudDetectionException;
import com.lokesh.fraud.transaction.dto.TransactionRequest;
import com.lokesh.fraud.transaction.dto.TransactionResponse;
import com.lokesh.fraud.transaction.dto.TransactionSearchCriteria;
import com.lokesh.fraud.transaction.dto.TransactionStatsResponse;
import com.lokesh.fraud.transaction.entity.TransactionEntity;
import com.lokesh.fraud.transaction.kafka.TransactionEventProducer;
import com.lokesh.fraud.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionValidationService validationService;
    private final TransactionEventProducer transactionEventProducer;

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        validationService.validateTransaction(request);
        String transactionId = UUID.randomUUID().toString();
        String masked = validationService.maskCardLastFour(request.getCardNumber());
        TransactionEntity entity = TransactionEntity.builder()
                .transactionId(transactionId)
                .accountId(request.getAccountId().trim())
                .amount(request.getAmount())
                .currency(request.getCurrency().trim().toUpperCase())
                .merchantId(request.getMerchantId().trim())
                .merchantCategory(request.getMerchantCategory().trim())
                .maskedCardNumber(masked)
                .sourceIp(request.getSourceIp().trim())
                .geoLocation(request.getGeoLocation().trim())
                .channel(request.getChannel().trim())
                .status(TransactionStatus.PENDING)
                .build();
        TransactionEntity saved = transactionRepository.save(entity);
        TransactionEvent event = new TransactionEvent(
                UUID.randomUUID().toString(),
                saved.getTransactionId(),
                saved.getAccountId(),
                saved.getAmount(),
                saved.getCurrency(),
                saved.getMerchantId(),
                saved.getMerchantCategory(),
                saved.getMaskedCardNumber(),
                saved.getSourceIp(),
                saved.getGeoLocation(),
                saved.getChannel(),
                saved.getCreatedAt()
        );
        transactionEventProducer.publishTransaction(event);
        return TransactionResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId)
                .map(TransactionResponse::fromEntity)
                .orElseThrow(() -> new FraudDetectionException("Transaction not found: " + transactionId));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> searchTransactions(TransactionSearchCriteria criteria) {
        List<TransactionEntity> results = transactionRepository.findBySearchCriteria(
                emptyToNull(criteria.getAccountId()),
                emptyToNull(criteria.getMerchantId()),
                criteria.getStatus(),
                criteria.getMinAmount(),
                criteria.getMaxAmount(),
                criteria.getStartDate(),
                criteria.getEndDate()
        );
        return results.stream().map(TransactionResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public TransactionResponse updateTransactionStatus(String transactionId, TransactionStatus status) {
        TransactionEntity entity = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new FraudDetectionException("Transaction not found: " + transactionId));
        entity.setStatus(status);
        if (status == TransactionStatus.FLAGGED || status == TransactionStatus.BLOCKED) {
            entity.setFlaggedAt(LocalDateTime.now());
        }
        return TransactionResponse.fromEntity(transactionRepository.save(entity));
    }

    @Transactional
    public void applyFraudAssessment(String transactionId, double riskScore, com.lokesh.fraud.common.enums.RiskLevel level) {
        TransactionEntity entity = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new FraudDetectionException("Transaction not found: " + transactionId));
        entity.setRiskScore(riskScore);
        TransactionStatus status = switch (level) {
            case LOW -> TransactionStatus.APPROVED;
            case MEDIUM, HIGH -> TransactionStatus.FLAGGED;
            case CRITICAL -> TransactionStatus.BLOCKED;
        };
        entity.setStatus(status);
        if (status == TransactionStatus.FLAGGED || status == TransactionStatus.BLOCKED) {
            entity.setFlaggedAt(LocalDateTime.now());
        }
        transactionRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public TransactionStatsResponse getTransactionStats() {
        List<TransactionEntity> all = transactionRepository.findAll();
        Map<TransactionStatus, Long> counts = new EnumMap<>(TransactionStatus.class);
        for (TransactionStatus s : TransactionStatus.values()) {
            counts.put(s, 0L);
        }
        BigDecimal volume = BigDecimal.ZERO;
        for (TransactionEntity t : all) {
            counts.merge(t.getStatus(), 1L, Long::sum);
            volume = volume.add(t.getAmount());
        }
        LocalDateTime dayAgo = LocalDateTime.now().minusHours(24);
        long flaggedRecent = transactionRepository.countByStatusAndCreatedAtAfter(TransactionStatus.FLAGGED, dayAgo)
                + transactionRepository.countByStatusAndCreatedAtAfter(TransactionStatus.BLOCKED, dayAgo);
        return TransactionStatsResponse.builder()
                .totalCount(all.size())
                .countByStatus(counts)
                .totalVolume(volume)
                .flaggedLast24h(flaggedRecent)
                .build();
    }

    private static String emptyToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
