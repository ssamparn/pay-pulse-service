package com.paypulse.platform.paypulsewebapi.dto.response;

import com.paypulse.platform.paypulsewebapi.dto.common.BatchStatus;
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