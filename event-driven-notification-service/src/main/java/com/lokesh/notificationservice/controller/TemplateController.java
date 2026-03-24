package com.lokesh.notificationservice.controller;

import com.lokesh.notificationservice.model.NotificationTemplate;
import com.lokesh.notificationservice.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Tag(name = "Templates", description = "Manage notification templates")
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    @Operation(summary = "Create a template")
    public ResponseEntity<NotificationTemplate> create(@Valid @RequestBody NotificationTemplate template) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.createTemplate(template));
    }

    @GetMapping
    @Operation(summary = "List all templates")
    public ResponseEntity<List<NotificationTemplate>> list() {
        return ResponseEntity.ok(templateService.getTemplates());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template by id")
    public ResponseEntity<NotificationTemplate> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(templateService.getTemplate(id));
    }
}
