package com.paypulse.platform.infrastructure.soap;

import java.time.LocalDateTime;
import java.util.List;

public record SoapBatchProcessingResult(
        String batchId,
        List<SoapTransactionResult> transactions,
        LocalDateTime processedAt
) {

    public record SoapTransactionResult(
            String externalPaymentId,
            TransactionOutcome outcome,
            boolean retryable,
            String failureReason,
            LocalDateTime processedAt
    ) {
    }

    public enum TransactionOutcome {
        SUCCESS,
        FAILED
    }
}

