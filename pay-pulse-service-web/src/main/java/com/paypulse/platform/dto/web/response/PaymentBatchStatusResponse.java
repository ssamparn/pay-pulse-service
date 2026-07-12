package com.paypulse.platform.dto.web.response;

import com.paypulse.platform.dto.common.BatchStatus;

import java.time.LocalDateTime;

public record PaymentBatchStatusResponse(
        String batchId,
        BatchStatus status,
        Summary summary,
        Timing timing,
        FailureInfo failureInfo,
        Links links
) {
    public record Summary(
            Integer totalTransactions,
            Integer successfulTransactions,
            Integer failedTransactions,
            Integer pendingTransactions
    ) {
    }

    public record Timing(
            LocalDateTime createdAt,
            LocalDateTime lastUpdatedAt,
            LocalDateTime estimatedCompletionTime
    ) {
    }

    public record FailureInfo(
            Integer retryableFailures,
            Integer permanentFailures,
            String lastErrorMessage
    ) {
    }

    public record Links(
            String paymentDetails,
            String failedPayments
    ) {
    }
}
