package com.lokesh.fraud.alert.repository;

import com.lokesh.fraud.common.enums.AlertStatus;
import com.lokesh.fraud.common.enums.RiskLevel;
import com.lokesh.fraud.alert.entity.AlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<AlertEntity, UUID> {

    Optional<AlertEntity> findByAlertId(String alertId);

    List<AlertEntity> findByAccountIdOrderByCreatedAtDesc(String accountId);

    List<AlertEntity> findByStatus(AlertStatus status);

    List<AlertEntity> findByRiskLevel(RiskLevel riskLevel);

    long countByRiskLevel(RiskLevel riskLevel);

    long countByStatus(AlertStatus status);

    List<AlertEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(a) FROM AlertEntity a WHERE a.status = :status AND a.resolvedAt >= :after")
    long countResolvedSince(@Param("status") AlertStatus status, @Param("after") LocalDateTime after);

    @Query("SELECT AVG(a.riskScore) FROM AlertEntity a")
    Double averageRiskScore();

    @Query("""
            SELECT a FROM AlertEntity a
            WHERE (:riskLevel IS NULL OR a.riskLevel = :riskLevel)
              AND (:status IS NULL OR a.status = :status)
              AND (:accountId IS NULL OR a.accountId = :accountId)
              AND (:startDate IS NULL OR a.createdAt >= :startDate)
              AND (:endDate IS NULL OR a.createdAt <= :endDate)
            ORDER BY a.createdAt DESC
            """)
    List<AlertEntity> search(
            @Param("riskLevel") RiskLevel riskLevel,
            @Param("status") AlertStatus status,
            @Param("accountId") String accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT a.riskLevel, COUNT(a) FROM AlertEntity a GROUP BY a.riskLevel")
    List<Object[]> countGroupedByRiskLevel();

    @Query(value = """
            SELECT CAST(created_at AS date) AS d, COUNT(*) AS c
            FROM alerts
            WHERE created_at >= (CURRENT_DATE - INTERVAL '13 day')
            GROUP BY CAST(created_at AS date)
            ORDER BY d ASC
            """, nativeQuery = true)
    List<Object[]> alertsTrendLast14Days();
}
