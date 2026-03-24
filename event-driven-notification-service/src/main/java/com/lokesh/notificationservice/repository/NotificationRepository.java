package com.lokesh.notificationservice.repository;

import com.lokesh.notificationservice.model.Notification;
import com.lokesh.notificationservice.model.NotificationChannel;
import com.lokesh.notificationservice.model.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientId(String recipientId);

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findByChannelAndStatus(NotificationChannel channel, NotificationStatus status);

    long countByStatusAndCreatedAtAfter(NotificationStatus status, LocalDateTime after);

    @Query("SELECT n.channel AS channel, COUNT(n) AS total FROM Notification n GROUP BY n.channel")
    List<ChannelCountProjection> countByChannelGrouped();

    long countByStatus(NotificationStatus status);

    interface ChannelCountProjection {
        NotificationChannel getChannel();

        Long getTotal();
    }
}
