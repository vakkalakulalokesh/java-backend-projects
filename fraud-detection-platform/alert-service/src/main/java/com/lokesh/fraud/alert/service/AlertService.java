package com.lokesh.fraud.alert.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.fraud.alert.dto.AlertResponse;
import com.lokesh.fraud.alert.dto.AlertSearchRequest;
import com.lokesh.fraud.alert.dto.AlertUpdateRequest;
import com.lokesh.fraud.alert.dto.DailyCount;
import com.lokesh.fraud.alert.dto.DashboardStats;
import com.lokesh.fraud.alert.entity.AlertEntity;
import com.lokesh.fraud.alert.repository.AlertRepository;
import com.lokesh.fraud.alert.websocket.AlertWebSocketHandler;
import com.lokesh.fraud.common.dto.FraudAlertEvent;
import com.lokesh.fraud.common.enums.AlertStatus;
import com.lokesh.fraud.common.enums.RiskLevel;
import com.lokesh.fraud.common.exception.FraudDetectionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final ObjectMapper objectMapper;
    private final AlertWebSocketHandler alertWebSocketHandler;

    @Transactional
    public AlertResponse createFromEvent(FraudAlertEvent event) {
        var existing = alertRepository.findByAlertId(event.alertId());
        if (existing.isPresent()) {
            return AlertResponse.fromEntity(existing.get());
        }
        String rulesJson;
        try {
            rulesJson = objectMapper.writeValueAsString(event.triggeredRules());
        } catch (JsonProcessingException e) {
            throw new FraudDetectionException("Failed to serialize triggered rules", e);
        }
        AlertEntity entity = AlertEntity.builder()
                .alertId(event.alertId())
                .transactionId(event.transactionId())
                .accountId(event.accountId())
                .riskScore(event.riskScore())
                .riskLevel(event.riskLevel())
                .triggeredRules(rulesJson)
                .recommendation(event.recommendation())
                .status(AlertStatus.NEW)
                .build();
        AlertEntity saved = alertRepository.save(entity);
        AlertResponse response = AlertResponse.fromEntity(saved);
        alertWebSocketHandler.broadcast(response);
        return response;
    }

    @Transactional(readOnly = true)
    public AlertResponse getAlert(String alertId) {
        return alertRepository.findByAlertId(alertId)
                .map(AlertResponse::fromEntity)
                .orElseThrow(() -> new FraudDetectionException("Alert not found: " + alertId));
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> searchAlerts(AlertSearchRequest request) {
        return alertRepository.search(
                request.getRiskLevel(),
                request.getStatus(),
                emptyToNull(request.getAccountId()),
                request.getStartDate(),
                request.getEndDate()
        ).stream().map(AlertResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public AlertResponse updateAlert(String alertId, AlertUpdateRequest request) {
        AlertEntity entity = alertRepository.findByAlertId(alertId)
                .orElseThrow(() -> new FraudDetectionException("Alert not found: " + alertId));
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
            if (request.getStatus() == AlertStatus.RESOLVED || request.getStatus() == AlertStatus.FALSE_POSITIVE) {
                entity.setResolvedAt(LocalDateTime.now());
            }
        }
        if (request.getAnalystNotes() != null) {
            entity.setAnalystNotes(request.getAnalystNotes());
        }
        if (request.getResolvedBy() != null) {
            entity.setResolvedBy(request.getResolvedBy());
        }
        return AlertResponse.fromEntity(alertRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        long total = alertRepository.count();
        long newCount = alertRepository.countByStatus(AlertStatus.NEW);
        long critical = alertRepository.countByRiskLevel(RiskLevel.CRITICAL);
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        long resolvedToday = alertRepository.countResolvedSince(AlertStatus.RESOLVED, startOfDay)
                + alertRepository.countResolvedSince(AlertStatus.FALSE_POSITIVE, startOfDay);
        Double avg = alertRepository.averageRiskScore();
        double avgScore = avg != null ? avg : 0;

        Map<RiskLevel, Long> byLevel = new EnumMap<>(RiskLevel.class);
        for (RiskLevel rl : RiskLevel.values()) {
            byLevel.put(rl, 0L);
        }
        for (Object[] row : alertRepository.countGroupedByRiskLevel()) {
            RiskLevel rl = (RiskLevel) row[0];
            long cnt = ((Number) row[1]).longValue();
            byLevel.put(rl, cnt);
        }

        List<DailyCount> trend = alertRepository.alertsTrendLast14Days().stream()
                .map(row -> DailyCount.builder()
                        .date(((java.sql.Date) row[0]).toLocalDate())
                        .count(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());

        return DashboardStats.builder()
                .totalAlerts(total)
                .newAlerts(newCount)
                .criticalAlerts(critical)
                .resolvedToday(resolvedToday)
                .avgRiskScore(avgScore)
                .alertsByRiskLevel(byLevel)
                .alertsTrend(trend)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getRecentAlerts(int limit) {
        int size = Math.min(Math.max(limit, 1), 500);
        return alertRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, size)).stream()
                .map(AlertResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private static String emptyToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
