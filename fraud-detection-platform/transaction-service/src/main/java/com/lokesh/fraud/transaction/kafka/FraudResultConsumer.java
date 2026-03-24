package com.lokesh.fraud.transaction.kafka;

import com.lokesh.fraud.common.dto.RiskAssessment;
import com.lokesh.fraud.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudResultConsumer {

    private final TransactionService transactionService;

    @KafkaListener(
            topics = "transactions.scored",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "riskAssessmentKafkaListenerContainerFactory"
    )
    public void onFraudResult(RiskAssessment assessment) {
        log.info("Received fraud assessment for transaction {} score {} level {}",
                assessment.transactionId(), assessment.riskScore(), assessment.riskLevel());
        transactionService.applyFraudAssessment(
                assessment.transactionId(),
                assessment.riskScore(),
                assessment.riskLevel()
        );
    }
}
