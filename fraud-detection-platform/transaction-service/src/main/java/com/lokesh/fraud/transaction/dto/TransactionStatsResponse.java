package com.lokesh.fraud.transaction.dto;

import com.lokesh.fraud.common.enums.TransactionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class TransactionStatsResponse {

    private long totalCount;
    private Map<TransactionStatus, Long> countByStatus;
    private BigDecimal totalVolume;
    private long flaggedLast24h;
}
