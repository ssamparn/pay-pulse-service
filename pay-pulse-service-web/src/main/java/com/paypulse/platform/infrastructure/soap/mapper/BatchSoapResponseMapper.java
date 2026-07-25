package com.paypulse.platform.infrastructure.soap.mapper;

import com.paypulse.platform.infrastructure.soap.SoapBatchProcessingResult;
import com.paypulse.platform.infrastructure.soap.model.rpy.SubmitBatchRpy;
import com.paypulse.platform.infrastructure.soap.model.rpy.SubmitPaymentTxnRpy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
public class BatchSoapResponseMapper {

    public SoapBatchProcessingResult toSoapBatchProcessingResult(SubmitBatchRpy response) {
        LocalDateTime batchProcessedAt = parseDateTime(response.getProcessedAt(), LocalDateTime.now());

        List<SoapBatchProcessingResult.SoapTransactionResult> transactions = response.getTransactions().stream()
                .map(this::toTransactionResult)
                .toList();

        return new SoapBatchProcessingResult(response.getBatchId(), transactions, batchProcessedAt);
    }

    private SoapBatchProcessingResult.SoapTransactionResult toTransactionResult(SubmitPaymentTxnRpy response) {
        SoapBatchProcessingResult.TransactionOutcome outcome =
                "SUCCESS".equalsIgnoreCase(response.getOutcome())
                        ? SoapBatchProcessingResult.TransactionOutcome.SUCCESS
                        : SoapBatchProcessingResult.TransactionOutcome.FAILED;

        LocalDateTime processedAt = parseDateTime(response.getProcessedAt(), LocalDateTime.now());

        return new SoapBatchProcessingResult.SoapTransactionResult(
                response.getExternalPaymentId(),
                outcome,
                response.isRetryable(),
                response.getFailureReason(),
                processedAt
        );
    }

    private LocalDateTime parseDateTime(String dateTime, LocalDateTime fallback) {
        if (dateTime == null || dateTime.isBlank()) {
            return fallback;
        }
        try {
            return LocalDateTime.parse(dateTime);
        } catch (DateTimeParseException exception) {
            return fallback;
        }
    }
}

