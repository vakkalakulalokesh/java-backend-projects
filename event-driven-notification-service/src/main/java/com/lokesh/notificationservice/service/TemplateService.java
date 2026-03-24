package com.lokesh.notificationservice.service;

import com.lokesh.notificationservice.exception.NotificationException;
import com.lokesh.notificationservice.model.NotificationTemplate;
import com.lokesh.notificationservice.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*}}");

    private final NotificationTemplateRepository templateRepository;
    private final CachedTemplateResolver cachedTemplateResolver;

    public NotificationTemplate getTemplateEntity(String templateId) {
        return cachedTemplateResolver.find(templateId);
    }

    @Transactional(readOnly = true)
    public String renderTemplate(String templateId, Map<String, String> params) {
        NotificationTemplate template = getTemplateEntity(templateId);
        if (template == null) {
            throw new NotificationException("Template not found: " + templateId);
        }
        if (!template.isActive()) {
            throw new NotificationException("Template inactive: " + templateId);
        }
        String body = template.getBodyTemplate();
        Matcher m = PLACEHOLDER.matcher(body);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            String value = params != null && params.containsKey(key) ? params.get(key) : "";
            m.appendReplacement(sb, Matcher.quoteReplacement(value == null ? "" : value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    @Transactional
    @CacheEvict(cacheNames = {"templates", "templatesById"}, allEntries = true)
    public NotificationTemplate createTemplate(NotificationTemplate template) {
        return templateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplate> getTemplates() {
        return templateRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "templatesById", key = "#id")
    public NotificationTemplate getTemplate(UUID id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new NotificationException("Template not found: " + id));
    }
}
