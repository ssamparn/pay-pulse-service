package com.paypulse.platform.infrastructure.soap.mapper;

import com.paypulse.platform.common.dto.BatchStatus;
import com.paypulse.platform.common.dto.PaymentMethod;
import com.paypulse.platform.infrastructure.soap.HistoricalSoapBatchSnapshot;
import com.paypulse.platform.infrastructure.soap.model.rpy.HistoricalBatchRpy;
import com.paypulse.platform.infrastructure.soap.model.rpy.HistoricalPaymentTxnRpy;
import com.paypulse.platform.infrastructure.soap.model.rpy.RetrieveHistoricalBatchesRpy;
import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import com.paypulse.platform.persistence.entity.PaymentTransactionEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class HistoricalSoapResponseMapper {

    public List<HistoricalSoapBatchSnapshot> toSnapshots(RetrieveHistoricalBatchesRpy response) {
        List<HistoricalSoapBatchSnapshot> snapshots = new ArrayList<>();

        if (response == null || response.getBatches() == null) {
            return snapshots;
        }

        for (HistoricalBatchRpy soapBatch : response.getBatches()) {
            PaymentBatchEntity batch = mapBatch(soapBatch);
            List<PaymentTransactionEntity> transactions = mapTransactions(soapBatch, batch);
            snapshots.add(new HistoricalSoapBatchSnapshot(batch, transactions));
        }

        return snapshots;
    }

    private PaymentBatchEntity mapBatch(HistoricalBatchRpy soapBatch) {
        LocalDateTime createdAt = parseDateTime(soapBatch.getCreatedAt(), LocalDateTime.now().minusDays(1));
        LocalDateTime updatedAt = parseDateTime(soapBatch.getLastUpdatedAt(), createdAt.plusMinutes(1));
        BatchStatus status = parseBatchStatus(soapBatch.getStatus(), BatchStatus.PENDING);

        int totalTransactions = defaultNumber(soapBatch.getPaymentCount());
        int successfulTransactions = Math.min(defaultNumber(soapBatch.getSuccessfulPayments()), totalTransactions);
        int failedTransactions = Math.min(defaultNumber(soapBatch.getFailedPayments()), totalTransactions - successfulTransactions);
        int pendingTransactions = Math.max(totalTransactions - successfulTransactions - failedTransactions, 0);

        LocalDateTime completedAt = parseDateTime(soapBatch.getCompletedAt(), null);
        if (isTerminal(status) && completedAt == null) {
            completedAt = updatedAt;
        }
        if (!isTerminal(status)) {
            completedAt = null;
        }

        return PaymentBatchEntity.create()
                .batchId(defaultValue(soapBatch.getBatchId(), generateFallbackBatchId(createdAt)))
                .merchantId(defaultValue(soapBatch.getMerchantId(), "MERCHANT-HIST-SOAP"))
                .customerId(defaultValue(soapBatch.getCustomerId(), "CUSTOMER-HIST-SOAP"))
                .externalBatchId(defaultValue(soapBatch.getExternalBatchId(), "EXT-" + defaultValue(soapBatch.getBatchId(), "UNKNOWN")))
                .status(status)
                .totalAmount(parseAmount(soapBatch.getTotalAmount(), BigDecimal.ZERO))
                .currency(normalizeCurrency(soapBatch.getCurrency()))
                .paymentMethod(parsePaymentMethod(soapBatch.getPaymentMethod(), PaymentMethod.SEPA))
                .executionDate(createdAt.toLocalDate())
                .batchDescription("Historical SOAP imported batch")
                .requestedBy("historical-soap-sync")
                .paymentsCount(totalTransactions)
                .totalTransactions(totalTransactions)
                .successfulTransactions(successfulTransactions)
                .failedTransactions(failedTransactions)
                .pendingTransactions(pendingTransactions)
                .progressPercentage(progressFromCounts(successfulTransactions, totalTransactions))
                .recoveryAttemptCount(0)
                .createdAt(createdAt)
                .completedAt(completedAt)
                .updatedAt(updatedAt)
                .build();
    }

    private List<PaymentTransactionEntity> mapTransactions(HistoricalBatchRpy soapBatch, PaymentBatchEntity mappedBatch) {
        if (soapBatch.getTransactions() == null) {
            return List.of();
        }

        List<PaymentTransactionEntity> transactions = new ArrayList<>(soapBatch.getTransactions().size());

        for (int i = 0; i < soapBatch.getTransactions().size(); i++) {
            HistoricalPaymentTxnRpy soapTxn = soapBatch.getTransactions().get(i);
            transactions.add(mapTransaction(soapTxn, mappedBatch, i + 1));
        }

        return transactions;
    }

    private PaymentTransactionEntity mapTransaction(HistoricalPaymentTxnRpy soapTxn, PaymentBatchEntity mappedBatch, int position) {
        LocalDateTime createdAt = mappedBatch.getCreatedAt();
        LocalDateTime updatedAt = parseDateTime(soapTxn.getUpdatedAt(), mappedBatch.getUpdatedAt());

        return PaymentTransactionEntity.create()
                .paymentId(defaultValue(soapTxn.getPaymentId(), mappedBatch.getBatchId() + "-PAY-" + position))
                .beneficiaryId(defaultValue(soapTxn.getBeneficiaryId(), "BEN-HIST-" + position))
                .batchId(mappedBatch.getBatchId())
                .beneficiaryName(defaultValue(soapTxn.getBeneficiaryName(), "Historical Beneficiary"))
                .beneficiaryIBAN(defaultValue(soapTxn.getBeneficiaryIbanMasked(), "DE00MASKED000000000000000000000000"))
                .externalPaymentId(defaultValue(soapTxn.getExternalPaymentId(), mappedBatch.getExternalBatchId() + "-TXN-" + position))
                .amount(parseAmount(soapTxn.getAmount(), new BigDecimal("0.01")))
                .currency(normalizeCurrency(soapTxn.getCurrency()))
                .paymentReference(defaultValue(soapTxn.getPaymentReference(), "HIST-REF-" + position))
                .failureReason(soapTxn.getFailureReason())
                .retryable(Boolean.TRUE.equals(soapTxn.getRetryable()))
                .status(parseTransactionStatus(soapTxn.getStatus()))
                .createdAt(createdAt)
                .processedAt(parseDateTime(soapTxn.getProcessedAt(), null))
                .updatedAt(updatedAt)
                .build();
    }

    private BatchStatus parseTransactionStatus(String value) {
        if (value == null || value.isBlank()) {
            return BatchStatus.PENDING;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("SUCCESS".equals(normalized) || "COMPLETED".equals(normalized)) {
            return BatchStatus.COMPLETED;
        }
        if ("FAILED".equals(normalized) || "PARTIALLY_COMPLETED".equals(normalized)) {
            return BatchStatus.FAILED;
        }
        if ("PROCESSING".equals(normalized)) {
            return BatchStatus.PROCESSING;
        }
        return BatchStatus.PENDING;
    }

    private BatchStatus parseBatchStatus(String value, BatchStatus fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return BatchStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private PaymentMethod parsePaymentMethod(String value, PaymentMethod fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return PaymentMethod.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private BigDecimal parseAmount(String value, BigDecimal fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            BigDecimal parsed = new BigDecimal(value);
            return parsed.compareTo(BigDecimal.ZERO) > 0 ? parsed : fallback;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private LocalDateTime parseDateTime(String value, LocalDateTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            return fallback;
        }
    }

    private int defaultNumber(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private int progressFromCounts(int successfulTransactions, int totalTransactions) {
        if (totalTransactions <= 0) {
            return 0;
        }
        return (int) Math.round((successfulTransactions * 100.0d) / totalTransactions);
    }

    private String normalizeCurrency(String value) {
        return value == null || value.isBlank() ? "EUR" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String generateFallbackBatchId(LocalDateTime createdAt) {
        LocalDate date = createdAt.toLocalDate();
        return "BP-HIST-" + date.toString().replace("-", "");
    }

    private String generateHistoricalIdempotencyKey(String batchId, String externalBatchId) {
        String token = defaultValue(batchId, "UNKNOWN") + "-" + defaultValue(externalBatchId, "UNKNOWN");
        String sanitized = token.replaceAll("[^A-Za-z0-9-]", "");
        if (sanitized.length() <= 56) {
            return "HIST-" + sanitized;
        }
        return "HIST-" + sanitized.substring(0, 56);
    }

    private boolean isTerminal(BatchStatus status) {
        return status == BatchStatus.COMPLETED
                || status == BatchStatus.FAILED
                || status == BatchStatus.PARTIALLY_COMPLETED;
    }
}
