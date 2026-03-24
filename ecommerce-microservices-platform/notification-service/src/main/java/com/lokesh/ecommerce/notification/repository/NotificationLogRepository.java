package com.lokesh.ecommerce.notification.repository;

import com.lokesh.ecommerce.notification.entity.DeliveryStatus;
import com.lokesh.ecommerce.notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    List<NotificationLog> findByOrderId(String orderId);

    List<NotificationLog> findByCustomerId(String customerId);

    long countByStatus(DeliveryStatus status);
}
