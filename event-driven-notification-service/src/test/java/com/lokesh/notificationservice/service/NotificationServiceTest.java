package com.lokesh.notificationservice.service;

import com.lokesh.notificationservice.config.TestNotificationProducerConfiguration;
import com.lokesh.notificationservice.dto.BulkNotificationRequest;
import com.lokesh.notificationservice.dto.NotificationRequest;
import com.lokesh.notificationservice.model.NotificationChannel;
import com.lokesh.notificationservice.model.NotificationPriority;
import com.lokesh.notificationservice.model.NotificationTemplate;
import com.lokesh.notificationservice.repository.NotificationRepository;
import com.lokesh.notificationservice.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestNotificationProducerConfiguration.class)
@EmbeddedKafka(
        partitions = 1,
        topics = {
                "notification.high-priority",
                "notification.standard",
                "notification.bulk",
                "notification.dead-letter"
        })
@Transactional
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationTemplateRepository notificationTemplateRepository;

    @BeforeEach
    void resetCounters() {
        TestNotificationProducerConfiguration.resetCounters();
    }

    @Test
    void send_persistsAndInvokesProducer() {
        NotificationRequest request = NotificationRequest.builder()
                .recipientId("user-1")
                .channel(NotificationChannel.EMAIL)
                .priority(NotificationPriority.MEDIUM)
                .subject("Hello")
                .content("Body")
                .build();

        var response = notificationService.send(request);

        assertThat(response.recipientId()).isEqualTo("user-1");
        assertThat(response.id()).isNotNull();
        assertThat(notificationRepository.findById(response.id())).isPresent();
        assertThat(TestNotificationProducerConfiguration.sendNotificationInvocations()).isEqualTo(1);
    }

    @Test
    void sendBulk_publishesBatch() {
        BulkNotificationRequest bulk = BulkNotificationRequest.builder()
                .notifications(List.of(
                        NotificationRequest.builder()
                                .recipientId("a")
                                .channel(NotificationChannel.SMS)
                                .content("x")
                                .build(),
                        NotificationRequest.builder()
                                .recipientId("b")
                                .channel(NotificationChannel.PUSH)
                                .content("y")
                                .build()))
                .build();

        var responses = notificationService.sendBulk(bulk);

        assertThat(responses).hasSize(2);
        assertThat(TestNotificationProducerConfiguration.sendBulkInvocations()).isEqualTo(1);
    }

    @Test
    void renderTemplate_replacesPlaceholders() {
        notificationTemplateRepository.save(NotificationTemplate.builder()
                .name("welcome")
                .channel(NotificationChannel.EMAIL)
                .subject("Hi {{name}}")
                .bodyTemplate("Hello {{name}}, code {{code}}")
                .active(true)
                .build());

        String body = templateService.renderTemplate("welcome", Map.of("name", "Ada", "code", "42"));

        assertThat(body).isEqualTo("Hello Ada, code 42");
    }
}
