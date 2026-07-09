package com.paypulse.platform.paypulsewebapi.dto;

import java.time.Instant;

public record PaymentBatchCreateResponse(
        String batchId,
        String status,
        Instant createdAt
) {
}

