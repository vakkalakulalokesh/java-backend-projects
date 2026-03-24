package com.lokesh.fraud.alert.kafka;

import com.lokesh.fraud.alert.service.AlertService;
import com.lokesh.fraud.common.dto.FraudAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudAlertConsumer {

    private final AlertService alertService;

    @KafkaListener(
            topics = "fraud.alerts",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "fraudAlertKafkaListenerContainerFactory"
    )
    public void onAlert(FraudAlertEvent event) {
        log.info("Persisting fraud alert {} for transaction {}", event.alertId(), event.transactionId());
        alertService.createFromEvent(event);
    }
}
