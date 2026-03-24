package com.lokesh.fraud.engine.rules;

import com.lokesh.fraud.common.dto.RuleResult;
import com.lokesh.fraud.common.dto.TransactionEvent;

public interface FraudRule {

    String getName();

    RuleResult evaluate(TransactionEvent transaction);

    int getPriority();
}
