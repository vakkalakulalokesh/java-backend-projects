package com.lokesh.ecommerce.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    public void send(String to, String subject, String body) {
        log.info("[EMAIL] to={} subject={} body={}", to, subject, body);
    }
}
