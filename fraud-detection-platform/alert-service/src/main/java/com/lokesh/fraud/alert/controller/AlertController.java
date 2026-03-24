package com.lokesh.fraud.alert.controller;

import com.lokesh.fraud.alert.dto.AlertResponse;
import com.lokesh.fraud.alert.dto.AlertSearchRequest;
import com.lokesh.fraud.alert.dto.AlertUpdateRequest;
import com.lokesh.fraud.alert.dto.DashboardStats;
import com.lokesh.fraud.alert.service.AlertService;
import com.lokesh.fraud.common.enums.AlertStatus;
import com.lokesh.fraud.common.enums.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Fraud alert operations and dashboard")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @Operation(summary = "Search alerts")
    public List<AlertResponse> search(
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        AlertSearchRequest req = AlertSearchRequest.builder()
                .riskLevel(riskLevel)
                .status(status)
                .accountId(accountId)
                .startDate(startDate)
                .endDate(endDate)
                .build();
        return alertService.searchAlerts(req);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get alert by id")
    public AlertResponse get(@PathVariable("id") String alertId) {
        return alertService.getAlert(alertId);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update alert status or analyst notes")
    public AlertResponse update(@PathVariable("id") String alertId, @Valid @RequestBody AlertUpdateRequest body) {
        return alertService.updateAlert(alertId, body);
    }

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Dashboard aggregates")
    public DashboardStats dashboard() {
        return alertService.getDashboardStats();
    }

    @GetMapping("/recent")
    @Operation(summary = "Most recent alerts")
    public List<AlertResponse> recent(@RequestParam(defaultValue = "20") int limit) {
        return alertService.getRecentAlerts(limit);
    }
}
