package com.lokesh.notificationservice.controller;

import com.lokesh.notificationservice.dto.NotificationResponse;
import com.lokesh.notificationservice.exception.GlobalExceptionHandler;
import com.lokesh.notificationservice.model.NotificationChannel;
import com.lokesh.notificationservice.model.NotificationPriority;
import com.lokesh.notificationservice.model.NotificationStatus;
import com.lokesh.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(notificationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void postNotification_returnsAccepted() throws Exception {
        UUID id = UUID.randomUUID();
        when(notificationService.send(any())).thenReturn(new NotificationResponse(
                id,
                "user-1",
                NotificationChannel.EMAIL,
                NotificationPriority.MEDIUM,
                NotificationStatus.PENDING,
                "Subj",
                LocalDateTime.now(),
                null));

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientId": "user-1",
                                  "channel": "EMAIL",
                                  "subject": "Subj",
                                  "content": "Hello"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void postBulk_returnsAccepted() throws Exception {
        when(notificationService.sendBulk(any())).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/notifications/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "notifications": [
                                    { "recipientId": "a", "channel": "SMS", "content": "x" }
                                  ]
                                }
                                """))
                .andExpect(status().isAccepted());
    }

    @Test
    void getById_returnsNotFoundWhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(notificationService.getNotification(id)).thenReturn(null);

        mockMvc.perform(get("/api/v1/notifications/" + id))
                .andExpect(status().isNotFound());
    }
}
