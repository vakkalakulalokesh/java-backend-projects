package com.lokesh.notificationservice.service;

import com.lokesh.notificationservice.model.NotificationTemplate;
import com.lokesh.notificationservice.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CachedTemplateResolver {

    private final NotificationTemplateRepository templateRepository;

    @Cacheable(value = "templates", key = "#templateId", unless = "#result == null")
    public NotificationTemplate find(String templateId) {
        try {
            UUID uuid = UUID.fromString(templateId);
            return templateRepository.findById(uuid).orElse(null);
        } catch (IllegalArgumentException ignored) {
            return templateRepository.findByName(templateId).orElse(null);
        }
    }
}
