package com.paypulse.platform.dto.web.response;

import com.paypulse.platform.dto.common.BatchStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for batch payment creation.
 */
public record BatchPaymentCreationResponse(
        String batchId,
        BatchStatus status,
        LocalDateTime createdAt,
        String statusUrl,
        boolean isDuplicate
) {
}