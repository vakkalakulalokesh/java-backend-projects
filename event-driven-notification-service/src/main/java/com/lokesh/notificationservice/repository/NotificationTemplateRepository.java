package com.lokesh.notificationservice.repository;

import com.lokesh.notificationservice.model.NotificationChannel;
import com.lokesh.notificationservice.model.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findByName(String name);

    List<NotificationTemplate> findByChannelAndActiveTrue(NotificationChannel channel);
}
