package com.lokesh.fraud.transaction.repository;

import com.lokesh.fraud.common.enums.TransactionStatus;
import com.lokesh.fraud.transaction.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    Optional<TransactionEntity> findByTransactionId(String transactionId);

    List<TransactionEntity> findByAccountId(String accountId);

    List<TransactionEntity> findByStatus(TransactionStatus status);

    long countByStatusAndCreatedAtAfter(TransactionStatus status, LocalDateTime after);

    @Query("""
            SELECT t FROM TransactionEntity t
            WHERE (:accountId IS NULL OR t.accountId = :accountId)
              AND (:merchantId IS NULL OR t.merchantId = :merchantId)
              AND (:status IS NULL OR t.status = :status)
              AND (:minAmount IS NULL OR t.amount >= :minAmount)
              AND (:maxAmount IS NULL OR t.amount <= :maxAmount)
              AND (:startDate IS NULL OR t.createdAt >= :startDate)
              AND (:endDate IS NULL OR t.createdAt <= :endDate)
            ORDER BY t.createdAt DESC
            """)
    List<TransactionEntity> findBySearchCriteria(
            @Param("accountId") String accountId,
            @Param("merchantId") String merchantId,
            @Param("status") TransactionStatus status,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
