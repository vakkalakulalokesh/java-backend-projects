package com.lokesh.notificationservice.controller;

import com.lokesh.notificationservice.dto.BulkNotificationRequest;
import com.lokesh.notificationservice.dto.NotificationRequest;
import com.lokesh.notificationservice.dto.NotificationResponse;
import com.lokesh.notificationservice.dto.NotificationStatsResponse;
import com.lokesh.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Create and query notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @Operation(summary = "Send a single notification", description = "Persists as PENDING and publishes to Kafka by priority")
    public ResponseEntity<NotificationResponse> send(@Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(notificationService.send(request));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Send bulk notifications", description = "Each item is persisted and published to the bulk topic")
    public ResponseEntity<List<NotificationResponse>> sendBulk(@Valid @RequestBody BulkNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(notificationService.sendBulk(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification by id")
    public ResponseEntity<NotificationResponse> getById(@PathVariable UUID id) {
        NotificationResponse r = notificationService.getNotification(id);
        if (r == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found: " + id);
        }
        return ResponseEntity.ok(r);
    }

    @GetMapping("/recipient/{recipientId}")
    @Operation(summary = "List notifications for a recipient")
    public ResponseEntity<List<NotificationResponse>> byRecipient(@PathVariable String recipientId) {
        return ResponseEntity.ok(notificationService.getNotificationsByRecipient(recipientId));
    }

    @GetMapping("/stats")
    @Operation(summary = "Aggregate counts by status and channel")
    public ResponseEntity<NotificationStatsResponse> stats() {
        return ResponseEntity.ok(notificationService.getNotificationStats());
    }
}
