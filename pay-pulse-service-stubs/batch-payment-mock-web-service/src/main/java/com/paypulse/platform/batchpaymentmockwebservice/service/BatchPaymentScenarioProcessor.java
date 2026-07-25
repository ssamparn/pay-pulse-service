package com.paypulse.platform.batchpaymentmockwebservice.service;

import com.paypulse.platform.batchpaymentmockwebservice.soap.model.req.SubmitBatchReq;
import com.paypulse.platform.batchpaymentmockwebservice.soap.model.req.SubmitPaymentTxnReq;
import com.paypulse.platform.batchpaymentmockwebservice.soap.model.rpy.SubmitBatchRpy;
import com.paypulse.platform.batchpaymentmockwebservice.soap.model.rpy.SubmitPaymentTxnRpy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

@Service
public class BatchPaymentScenarioProcessor {

    public SubmitBatchRpy processBatch(SubmitBatchReq request) {
        LocalDateTime now = LocalDateTime.now();
        SplittableRandom random = new SplittableRandom(seedFromBatch(request));
        LocalDate executionDate = parseExecutionDate(request.getExecutionDate());

        boolean downstreamOutage = containsIgnoreCase(request.getMerchantId(), "SOAP_DOWN")
                || containsIgnoreCase(request.getBatchId(), "SOAP_DOWN")
                || containsIgnoreCase(request.getIdempotencyKey(), "OUTAGE");

        Set<String> seenExternalPaymentIds = new HashSet<>();

        List<SubmitPaymentTxnRpy> transactionReplies = request.getTransactions().stream()
                .map(transaction -> evaluateTransaction(
                        request,
                        transaction,
                        seenExternalPaymentIds,
                        downstreamOutage,
                        executionDate,
                        now,
                        random
                ))
                .toList();

        SubmitBatchRpy response = new SubmitBatchRpy();
        response.setBatchId(request.getBatchId());
        response.setProcessedAt(now.toString());
        response.setTransactions(transactionReplies);
        return response;
    }

    private SubmitPaymentTxnRpy evaluateTransaction(
            SubmitBatchReq batch,
            SubmitPaymentTxnReq transaction,
            Set<String> seenExternalPaymentIds,
            boolean downstreamOutage,
            LocalDate executionDate,
            LocalDateTime now,
            SplittableRandom random
    ) {
        SubmitPaymentTxnRpy response = baseReply(transaction.getExternalPaymentId(), now.plusNanos(random.nextLong(1_000_000)));

        if (downstreamOutage) {
            return failure(response, true, "Downstream clearing network unavailable");
        }

        if (!seenExternalPaymentIds.add(transaction.getExternalPaymentId())) {
            return failure(response, false, "Duplicate externalPaymentId within batch");
        }

        BigDecimal amount = parseAmount(transaction.getAmount());
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return failure(response, false, "Invalid amount");
        }

        if (!"EUR".equalsIgnoreCase(transaction.getCurrency())) {
            return failure(response, false, "Unsupported currency");
        }

        if (!"SEPA".equalsIgnoreCase(batch.getPaymentMethod()) && amount.compareTo(new BigDecimal("5000")) > 0) {
            return failure(response, false, "Payment method limit exceeded");
        }

        if (containsIgnoreCase(transaction.getBeneficiaryIban(), "INVALID")
                || (transaction.getBeneficiaryIban() != null && transaction.getBeneficiaryIban().endsWith("0000"))) {
            return failure(response, false, "IBAN validation failed");
        }

        if (containsAnyIgnoreCase(transaction.getPaymentReference(), "ACCOUNT_CLOSED", "FROZEN", "BLACKLIST")) {
            return failure(response, false, "Beneficiary account not eligible");
        }

        if (containsAnyIgnoreCase(transaction.getPaymentReference(), "TIMEOUT", "RETRY", "UPSTREAM_TEMP")) {
            return failure(response, true, "Temporary upstream timeout");
        }

        if (amount.compareTo(new BigDecimal("10000")) >= 0) {
            return failure(response, true, "Manual compliance review pending");
        }

        if (isWeekend(executionDate) && random.nextInt(100) < 35) {
            return failure(response, true, "Clearing house closed for non-urgent weekend processing");
        }

        int riskScore = computeRiskScore(transaction, random);
        if (riskScore >= 85) {
            return failure(response, false, "Fraud risk threshold exceeded");
        }
        if (riskScore >= 65) {
            return failure(response, true, "Risk engine deferred for additional checks");
        }

        return success(response);
    }

    private SubmitPaymentTxnRpy baseReply(String externalPaymentId, LocalDateTime processedAt) {
        SubmitPaymentTxnRpy response = new SubmitPaymentTxnRpy();
        response.setExternalPaymentId(externalPaymentId);
        response.setProcessedAt(processedAt.toString());
        return response;
    }

    private SubmitPaymentTxnRpy success(SubmitPaymentTxnRpy response) {
        response.setOutcome("SUCCESS");
        response.setFailureReason(null);
        response.setRetryable(false);
        return response;
    }

    private SubmitPaymentTxnRpy failure(SubmitPaymentTxnRpy response, boolean retryable, String reason) {
        response.setOutcome("FAILED");
        response.setFailureReason(reason);
        response.setRetryable(retryable);
        return response;
    }

    private int computeRiskScore(SubmitPaymentTxnReq transaction, SplittableRandom random) {
        int score = random.nextInt(25, 70);

        String beneficiaryName = transaction.getBeneficiaryName();
        if (beneficiaryName != null && beneficiaryName.trim().split("\\s+").length <= 1) {
            score += 8;
        }

        String paymentReference = transaction.getPaymentReference();
        if (containsAnyIgnoreCase(paymentReference, "URGENT", "CASH", "CRYPTO", "GIFT")) {
            score += 20;
        }

        return Math.min(score, 100);
    }

    private long seedFromBatch(SubmitBatchReq request) {
        long seed = 17L;
        seed = 31 * seed + safeHash(request.getBatchId());
        seed = 31 * seed + safeHash(request.getIdempotencyKey());
        seed = 31 * seed + safeHash(request.getMerchantId());
        return seed;
    }

    private int safeHash(String value) {
        return value == null ? 0 : value.hashCode();
    }

    private LocalDate parseExecutionDate(String executionDate) {
        if (executionDate == null || executionDate.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(executionDate);
        } catch (DateTimeParseException exception) {
            return LocalDate.now();
        }
    }

    private BigDecimal parseAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(amount);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private boolean containsIgnoreCase(String value, String token) {
        if (value == null || token == null) {
            return false;
        }
        return value.toLowerCase().contains(token.toLowerCase());
    }

    private boolean containsAnyIgnoreCase(String value, String... tokens) {
        for (String token : tokens) {
            if (containsIgnoreCase(value, token)) {
                return true;
            }
        }
        return false;
    }
}

