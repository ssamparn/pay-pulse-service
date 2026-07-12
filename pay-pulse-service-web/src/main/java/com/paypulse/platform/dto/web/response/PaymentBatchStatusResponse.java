package com.paypulse.platform.dto.web.response;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentBatchStatusResponse(
        String batchId,
        String clientReference,
        String status,
        int paymentCount,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt
) {
}

