package com.paypulse.platform.web.dto.response;

import com.paypulse.platform.common.dto.BatchStatus;

import java.time.LocalDateTime;

public record PaymentBatchCreateResponse(
        String batchId,
        BatchStatus status,
        LocalDateTime createdAt,
        String statusUrl,
        boolean isDuplicate
) {
}