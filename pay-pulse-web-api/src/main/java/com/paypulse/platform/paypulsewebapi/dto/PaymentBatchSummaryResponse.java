package com.paypulse.platform.paypulsewebapi.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentBatchSummaryResponse(
        String batchId,
        String status,
        int paymentCount,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt
) {
}

