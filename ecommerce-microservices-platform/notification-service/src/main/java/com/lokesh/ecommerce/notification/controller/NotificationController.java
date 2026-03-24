package com.lokesh.ecommerce.notification.controller;

import com.lokesh.ecommerce.notification.entity.NotificationLog;
import com.lokesh.ecommerce.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationLogRepository notificationLogRepository;

    @GetMapping("/order/{orderId}")
    public List<NotificationLog> byOrder(@PathVariable String orderId) {
        return notificationLogRepository.findByOrderId(orderId);
    }

    @GetMapping("/customer/{customerId}")
    public List<NotificationLog> byCustomer(@PathVariable String customerId) {
        return notificationLogRepository.findByCustomerId(customerId);
    }
}
