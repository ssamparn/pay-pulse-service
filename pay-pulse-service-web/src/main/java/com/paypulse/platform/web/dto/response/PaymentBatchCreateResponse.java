package com.paypulse.platform.web.dto.response;

import com.paypulse.platform.common.dto.BatchStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for batch payment creation.
 */
public record PaymentBatchCreateResponse(
        String batchId,
        BatchStatus status,
        LocalDateTime createdAt,
        String statusUrl,
        boolean isDuplicate
) {
}