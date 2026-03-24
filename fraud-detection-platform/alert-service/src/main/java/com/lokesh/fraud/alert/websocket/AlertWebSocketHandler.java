package com.lokesh.fraud.alert.websocket;

import com.lokesh.fraud.alert.dto.AlertResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(AlertResponse alert) {
        messagingTemplate.convertAndSend("/topic/alerts", alert);
        messagingTemplate.convertAndSend("/topic/alerts/" + alert.accountId(), alert);
        log.debug("Broadcast alert {} to STOMP topics", alert.alertId());
    }
}
