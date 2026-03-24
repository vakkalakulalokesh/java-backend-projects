package com.lokesh.fraud.engine.kafka;

import com.lokesh.fraud.common.dto.FraudAlertEvent;
import com.lokesh.fraud.common.dto.RiskAssessment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FraudResultProducer {

    private static final String SCORED_TOPIC = "transactions.scored";
    private static final String ALERT_TOPIC = "fraud.alerts";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public FraudResultProducer(@Qualifier("fraudKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishScored(RiskAssessment assessment) {
        kafkaTemplate.send(SCORED_TOPIC, assessment.transactionId(), assessment)
                .whenComplete((r, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish scored result for {}", assessment.transactionId(), ex);
                    } else {
                        log.debug("Published scored result for {}", assessment.transactionId());
                    }
                });
    }

    public void publishAlert(FraudAlertEvent alert) {
        kafkaTemplate.send(ALERT_TOPIC, alert.transactionId(), alert)
                .whenComplete((r, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish alert for {}", alert.transactionId(), ex);
                    } else {
                        log.info("Published fraud alert {} for transaction {}", alert.alertId(), alert.transactionId());
                    }
                });
    }
}
