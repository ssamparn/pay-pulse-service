package com.paypulse.platform.historicalbatchesmockwebservice.service;

import com.paypulse.platform.historicalbatchesmockwebservice.soap.model.req.RetrieveHistoricalBatchesReq;
import com.paypulse.platform.historicalbatchesmockwebservice.soap.model.rpy.HistoricalBatchRpy;
import com.paypulse.platform.historicalbatchesmockwebservice.soap.model.rpy.HistoricalPaymentTxnRpy;
import com.paypulse.platform.historicalbatchesmockwebservice.soap.model.rpy.RetrieveHistoricalBatchesRpy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;

@Service
public class HistoricalBatchScenarioProcessor {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    public RetrieveHistoricalBatchesRpy process(RetrieveHistoricalBatchesReq request) {
        if (containsIgnoreCase(request.getMerchantId(), "HIST_OUTAGE")) {
            throw new IllegalStateException("Historical data source unavailable");
        }

        LocalDateRange range = resolveRange(request);
        int page = sanitizePage(request.getPage());
        int pageSize = sanitizePageSize(request.getPageSize());
        boolean includeTransactions = Boolean.TRUE.equals(request.getIncludeTransactions());
        SplittableRandom random = new SplittableRandom(seedFromRequest(request, range));

        List<HistoricalBatchRpy> generated = generateBatches(request, range, random, includeTransactions);
        generated.sort(Comparator.comparing(HistoricalBatchRpy::getCreatedAt).reversed());

        int total = generated.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        int boundedPage = Math.min(page, totalPages);
        int from = Math.max((boundedPage - 1) * pageSize, 0);
        int to = Math.min(from + pageSize, total);

        List<HistoricalBatchRpy> pageItems = from >= to ? List.of() : generated.subList(from, to);

        RetrieveHistoricalBatchesRpy response = new RetrieveHistoricalBatchesRpy();
        response.setRequestId(buildRequestId(request, range));
        response.setGeneratedAt(LocalDateTime.now().toString());
        response.setFromDate(range.from().toString());
        response.setToDate(range.to().toString());
        response.setCurrentPage(boundedPage);
        response.setPageSize(pageSize);
        response.setTotalPages(totalPages);
        response.setTotalBatches(total);
        response.setSourceSystem(determineSourceSystem(request));
        if (containsIgnoreCase(request.getMerchantId(), "HIST_STALE")) {
            response.setStaleAsOf(LocalDateTime.now().minusHours(4).toString());
        }
        response.setBatches(pageItems);
        return response;
    }

    private List<HistoricalBatchRpy> generateBatches(
            RetrieveHistoricalBatchesReq request,
            LocalDateRange range,
            SplittableRandom random,
            boolean includeTransactions
    ) {
        if (containsIgnoreCase(request.getMerchantId(), "HIST_EMPTY")) {
            return List.of();
        }

        int days = (int) (range.to().toEpochDay() - range.from().toEpochDay() + 1);
        int baseVolume = containsIgnoreCase(request.getMerchantId(), "HIST_SPIKE") ? 8 : 3;
        int totalBatches = Math.max(2, days * baseVolume + random.nextInt(days + 3));

        List<HistoricalBatchRpy> batches = new ArrayList<>(totalBatches);
        for (int i = 0; i < totalBatches; i++) {
            LocalDate createdDate = range.from().plusDays(random.nextInt(days));
            LocalDateTime createdAt = createdDate.atTime(randomTime(random));

            int paymentCount = containsIgnoreCase(request.getMerchantId(), "HIST_LARGE")
                    ? 80 + random.nextInt(420)
                    : 2 + random.nextInt(64);

            BatchComposition composition = composeStatuses(request, createdAt.toLocalDate(), paymentCount, random);
            List<HistoricalPaymentTxnRpy> transactions = includeTransactions
                    ? buildTransactions(composition, createdAt, random)
                    : List.of();

            BigDecimal totalAmount = calculateTotalAmount(transactions, paymentCount, random);

            HistoricalBatchRpy batch = new HistoricalBatchRpy();
            batch.setBatchId(String.format("BP-%s-%05d", createdDate.toString().replace("-", ""), i + 1));
            batch.setExternalBatchId(String.format("EXT-HIST-%s-%04d", createdDate.toString().replace("-", ""), i + 1));
            batch.setMerchantId(defaultIfBlank(request.getMerchantId(), "MERCHANT-HIST-001"));
            batch.setCustomerId(defaultIfBlank(request.getCustomerId(), "CUSTOMER-HIST-001"));
            batch.setStatus(composition.status());
            batch.setTotalAmount(totalAmount.toPlainString());
            batch.setCurrency("EUR");
            batch.setPaymentMethod(random.nextInt(100) < 90 ? "SEPA" : "SWIFT");
            batch.setPaymentCount(paymentCount);
            batch.setSuccessfulPayments(composition.successful());
            batch.setFailedPayments(composition.failed());
            batch.setPendingPayments(composition.pending());
            batch.setProgressPercentage(progress(composition.successful(), paymentCount));
            batch.setCreatedAt(createdAt.toString());
            batch.setLastUpdatedAt(createdAt.plusMinutes(3 + random.nextInt(180)).toString());
            batch.setCompletedAt(completedAt(composition.status(), createdAt, random));
            batch.setLastErrorMessage(lastError(composition.status(), random));
            batch.setStale(containsIgnoreCase(request.getMerchantId(), "HIST_STALE"));
            batch.setTransactions(transactions);
            batches.add(batch);
        }

        return batches;
    }

    private BatchComposition composeStatuses(
            RetrieveHistoricalBatchesReq request,
            LocalDate batchDate,
            int paymentCount,
            SplittableRandom random
    ) {
        boolean forcePartial = containsIgnoreCase(request.getMerchantId(), "HIST_PARTIAL_HEAVY");
        boolean recentWindow = batchDate.isAfter(LocalDate.now().minusDays(2));

        int pick = random.nextInt(100);
        String status;

        if (forcePartial && pick < 55) {
            status = "PARTIALLY_COMPLETED";
        } else if (recentWindow && pick < 25) {
            status = pick < 8 ? "PENDING" : "PROCESSING";
        } else if (pick < 68) {
            status = "COMPLETED";
        } else if (pick < 88) {
            status = "PARTIALLY_COMPLETED";
        } else if (pick < 95) {
            status = "FAILED";
        } else {
            status = "PROCESSING";
        }

        return switch (status) {
            case "PENDING" -> new BatchComposition(status, 0, 0, paymentCount);
            case "PROCESSING" -> {
                int maxSuccessful = Math.max(paymentCount - 1, 0);
                int successful = maxSuccessful == 0 ? 0 : random.nextInt(maxSuccessful + 1);
                int remainingAfterSuccess = paymentCount - successful;
                int maxFailed = Math.max(remainingAfterSuccess - 1, 0);
                int failed = maxFailed == 0 ? 0 : random.nextInt(maxFailed + 1);
                int pending = paymentCount - successful - failed;
                yield new BatchComposition(status, successful, failed, pending);
            }
            case "FAILED" -> new BatchComposition(status, 0, paymentCount, 0);
            case "PARTIALLY_COMPLETED" -> {
                int failed = Math.max(1, random.nextInt(Math.max(2, paymentCount / 3)));
                int successful = Math.max(1, paymentCount - failed);
                yield new BatchComposition(status, successful, failed, 0);
            }
            default -> new BatchComposition("COMPLETED", paymentCount, 0, 0);
        };
    }

    private List<HistoricalPaymentTxnRpy> buildTransactions(
            BatchComposition composition,
            LocalDateTime createdAt,
            SplittableRandom random
    ) {
        int total = composition.successful() + composition.failed() + composition.pending();
        List<HistoricalPaymentTxnRpy> transactions = new ArrayList<>(total);

        for (int i = 0; i < total; i++) {
            String txnStatus = statusForIndex(i, composition);
            LocalDateTime updatedAt = createdAt.plusMinutes(1 + random.nextInt(240));

            HistoricalPaymentTxnRpy txn = new HistoricalPaymentTxnRpy();
            txn.setPaymentId(String.format("PAY-%05d", i + 1));
            txn.setExternalPaymentId(String.format("EXT-PAY-HIST-%05d", i + 1));
            txn.setBeneficiaryId(String.format("BEN-%04d", 1000 + random.nextInt(9000)));
            txn.setBeneficiaryName("Beneficiary " + (char) ('A' + random.nextInt(26)));
            txn.setBeneficiaryIbanMasked(maskedIban(random));
            txn.setAmount(new BigDecimal(80 + random.nextInt(4200)).setScale(2, RoundingMode.HALF_UP).toPlainString());
            txn.setCurrency("EUR");
            txn.setPaymentReference("HIST-INV-" + (10000 + random.nextInt(90000)));
            txn.setStatus(txnStatus);
            txn.setRetryable("FAILED".equals(txnStatus) && random.nextInt(100) < 30);
            txn.setFailureReason("FAILED".equals(txnStatus) ? failureReason(random) : null);
            txn.setProcessedAt("PENDING".equals(txnStatus) ? null : updatedAt.toString());
            txn.setUpdatedAt(updatedAt.toString());
            transactions.add(txn);
        }

        return transactions;
    }

    private BigDecimal calculateTotalAmount(List<HistoricalPaymentTxnRpy> transactions, int paymentCount, SplittableRandom random) {
        if (transactions.isEmpty()) {
            BigDecimal avg = new BigDecimal(250 + random.nextInt(2200));
            return avg.multiply(BigDecimal.valueOf(paymentCount)).setScale(2, RoundingMode.HALF_UP);
        }

        return transactions.stream()
                .map(HistoricalPaymentTxnRpy::getAmount)
                .map(BigDecimal::new)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String statusForIndex(int index, BatchComposition composition) {
        if (index < composition.successful()) {
            return "SUCCESS";
        }
        if (index < composition.successful() + composition.failed()) {
            return "FAILED";
        }
        return "PENDING";
    }

    private String completedAt(String status, LocalDateTime createdAt, SplittableRandom random) {
        if ("COMPLETED".equals(status) || "FAILED".equals(status) || "PARTIALLY_COMPLETED".equals(status)) {
            return createdAt.plusMinutes(5 + random.nextInt(320)).toString();
        }
        return null;
    }

    private String lastError(String status, SplittableRandom random) {
        if (!"FAILED".equals(status) && !"PARTIALLY_COMPLETED".equals(status)) {
            return null;
        }
        return switch (random.nextInt(4)) {
            case 0 -> "IBAN validation failed";
            case 1 -> "Beneficiary account blocked";
            case 2 -> "Upstream bank timeout";
            default -> "Compliance review timeout";
        };
    }

    private int progress(int successful, int total) {
        if (total == 0) {
            return 0;
        }
        return (int) Math.round((successful * 100.0d) / total);
    }

    private String failureReason(SplittableRandom random) {
        return switch (random.nextInt(5)) {
            case 0 -> "Invalid beneficiary IBAN";
            case 1 -> "Limit exceeded";
            case 2 -> "Compliance hold";
            case 3 -> "Beneficiary account frozen";
            default -> "Technical timeout";
        };
    }

    private String determineSourceSystem(RetrieveHistoricalBatchesReq request) {
        if (containsIgnoreCase(request.getMerchantId(), "HIST_DR")) {
            return "HISTORICAL_DR_REGION";
        }
        return "HISTORICAL_PRIMARY_REGION";
    }

    private LocalDateRange resolveRange(RetrieveHistoricalBatchesReq request) {
        LocalDate today = LocalDate.now();

        if (request.getPeriod() != null && !request.getPeriod().isBlank()) {
            String period = request.getPeriod().trim().toUpperCase();
            if ("LAST_3_MONTHS".equals(period)) {
                return new LocalDateRange(today.minusMonths(3), today);
            }
            if ("LAST_6_MONTHS".equals(period)) {
                return new LocalDateRange(today.minusMonths(6), today);
            }
        }

        LocalDate from = parseDateOrDefault(request.getFromDate(), today.minusMonths(3));
        LocalDate to = parseDateOrDefault(request.getToDate(), today);

        if (from.isAfter(to)) {
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }

        if (to.minusDays(180).isAfter(from)) {
            from = to.minusDays(180);
        }

        return new LocalDateRange(from, to);
    }

    private LocalDate parseDateOrDefault(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return fallback;
        }
    }

    private String buildRequestId(RetrieveHistoricalBatchesReq request, LocalDateRange range) {
        return String.format(
                "HIST-%s-%s-%s",
                range.from().toString().replace("-", ""),
                range.to().toString().replace("-", ""),
                Integer.toHexString(seedFromRequest(request, range))
        );
    }

    private int sanitizePage(Integer page) {
        if (page == null || page < 1) {
            return 1;
        }
        return page;
    }

    private int sanitizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private LocalTime randomTime(SplittableRandom random) {
        return LocalTime.of(random.nextInt(24), random.nextInt(60), random.nextInt(60));
    }

    private String maskedIban(SplittableRandom random) {
        return "DE89**************" + (1000 + random.nextInt(9000));
    }

    private int seedFromRequest(RetrieveHistoricalBatchesReq request, LocalDateRange range) {
        int seed = 19;
        seed = 31 * seed + safeHash(request.getMerchantId());
        seed = 31 * seed + safeHash(request.getCustomerId());
        seed = 31 * seed + safeHash(request.getPeriod());
        seed = 31 * seed + safeHash(range.from().toString());
        seed = 31 * seed + safeHash(range.to().toString());
        return seed;
    }

    private int safeHash(String value) {
        return value == null ? 0 : value.hashCode();
    }

    private boolean containsIgnoreCase(String input, String token) {
        if (input == null || token == null) {
            return false;
        }
        return input.toLowerCase().contains(token.toLowerCase());
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record BatchComposition(String status, int successful, int failed, int pending) {
    }

    private record LocalDateRange(LocalDate from, LocalDate to) {
    }
}
