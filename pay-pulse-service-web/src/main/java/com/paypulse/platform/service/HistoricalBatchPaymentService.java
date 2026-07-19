package com.paypulse.platform.service;

import com.paypulse.platform.dto.common.BatchStatus;
import com.paypulse.platform.dto.web.response.PaymentBatchListResponse;
import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import com.paypulse.platform.persistence.entity.PaymentTransactionEntity;
import com.paypulse.platform.persistence.repository.PaymentBatchRepository;
import com.paypulse.platform.persistence.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricalBatchPaymentService {

    private final PaymentBatchRepository paymentBatchRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    /**
     * Retrieves historical payment batches within specified time window.
     *
     * Behavior:
     * 1. Validate and parse filter parameters (period or custom date range)
     * 2. Check local database for cached batches
     * 3. If data not available locally, call SOAP historical API
     * 4. Cache retrieved records in PostgreSQL
     * 5. Apply pagination and return results with summary statistics
     *
     * @param period Predefined period (LAST_3_MONTHS, LAST_6_MONTHS)
     * @param fromDate Custom start date
     * @param toDate Custom end date
     * @param page Page number (1-indexed)
     * @param pageSize Results per page
     * @return PaymentBatchListResponse with batches and pagination
     */
    @Transactional(readOnly = true)
    public PaymentBatchListResponse getHistoricalBatches(
            String period, LocalDate fromDate, LocalDate toDate,
            Integer page, Integer pageSize) {

        log.debug("Retrieving historical batches. Period: {}, FromDate: {}, ToDate: {}", period, fromDate, toDate);

        // Step 1: Parse and validate filter parameters
        DateRange dateRange = parseDateRange(period, fromDate, toDate);
        log.debug("Calculated date range: {} to {}", dateRange.from(), dateRange.to());

        // Step 2: Check local database for cached batches
        List<PaymentBatchEntity> localBatches = new ArrayList<>(paymentBatchRepository.findAll().stream()
                .filter(batch -> !batch.getCreatedAt().isBefore(dateRange.from().atStartOfDay()))
                .filter(batch -> !batch.getCreatedAt().isAfter(dateRange.to().atTime(23, 59, 59)))
                .toList());
        log.debug("Found {} batches in local database", localBatches.size());

        // Step 3: If insufficient data, call SOAP historical API
        // SOAP integration is not wired yet, so rely on locally available data for now.
        if (localBatches.isEmpty() || isDataStale(dateRange)) {
            log.info("Historical SOAP sync is skipped; using local cache for period: {} to {}",
                    dateRange.from(), dateRange.to());
        }

        // Sort by created date descending
        localBatches.sort((b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt()));

        // Step 5: Apply pagination
        int totalRecords = localBatches.size();
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalRecords);

        List<PaymentBatchEntity> paginatedBatches = localBatches.subList(startIndex, endIndex);

        // Build batch items with summary stats
        List<PaymentBatchListResponse.BatchItem> batchItems = paginatedBatches.stream()
                .map(this::toBatchItem)
                .toList();

        // Calculate summary statistics
        PaymentBatchListResponse.Summary summary = calculateSummary(localBatches);

        // Build pagination info
        PaymentBatchListResponse.Pagination pagination = new PaymentBatchListResponse.Pagination(
                page,
                pageSize,
                totalPages,
                (long) totalRecords,
                page < totalPages,
                page > 1
        );

        // Build filters info
        PaymentBatchListResponse.Filters filters = new PaymentBatchListResponse.Filters(
                period,
                dateRange.from(),
                dateRange.to()
        );

        PaymentBatchListResponse response = new PaymentBatchListResponse(
                batchItems,
                pagination,
                filters,
                summary
        );

        log.info("Historical batches retrieved. Total: {}, Page: {}/{}, Period: {} to {}",
                totalRecords, page, totalPages, dateRange.from(), dateRange.to());

        return response;
    }

    /**
     * Converts PaymentBatch entity to BatchItem DTO.
     */
    private PaymentBatchListResponse.BatchItem toBatchItem(PaymentBatchEntity batch) {
        List<PaymentTransactionEntity> paymentEntities = paymentTransactionRepository.findByBatchId(batch.getBatchId());

        long successfulCount = paymentEntities.stream()
                .filter(p -> p.getStatus() == BatchStatus.COMPLETED)
                .count();

        long failedCount = paymentEntities.stream()
                .filter(p -> p.getStatus() == BatchStatus.FAILED)
                .count();

        return new PaymentBatchListResponse.BatchItem(
                batch.getBatchId(),
                batch.getExternalBatchId(),
                batch.getStatus(),
                batch.getTotalAmount(),
                batch.getCurrency(),
                batch.getPaymentMethod().toString(),
                paymentEntities.size(),
                (int) successfulCount,
                (int) failedCount,
                batch.getCreatedAt(),
                batch.getUpdatedAt(),
                "/api/v1/batch-payment/" + batch.getBatchId() + "/status",
                "/api/v1/batch-payment/" + batch.getBatchId() + "/payments"
        );
    }

    /**
     * Calculates summary statistics for batches.
     */
    private PaymentBatchListResponse.Summary calculateSummary(List<PaymentBatchEntity> batches) {
        long totalBatches = batches.size();

        BigDecimal totalAmount = batches.stream()
                .map(PaymentBatchEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long completedBatches = batches.stream()
                .filter(b -> b.getStatus() == BatchStatus.COMPLETED)
                .count();

        long partiallyCompletedBatches = batches.stream()
                .filter(b -> b.getStatus() == BatchStatus.PARTIALLY_COMPLETED)
                .count();

        long failedBatches = batches.stream()
                .filter(b -> b.getStatus() == BatchStatus.FAILED)
                .count();

        return new PaymentBatchListResponse.Summary(
                totalBatches,
                totalAmount,
                completedBatches,
                partiallyCompletedBatches,
                failedBatches
        );
    }

    /**
     * Parses date range from period or custom dates.
     */
    private DateRange parseDateRange(String period, LocalDate fromDate, LocalDate toDate) {
        LocalDate from, to;
        LocalDate today = LocalDate.now();

        if (period != null) {
            switch (period.toUpperCase()) {
                case "LAST_3_MONTHS":
                    from = today.minusMonths(3);
                    to = today;
                    break;
                case "LAST_6_MONTHS":
                    from = today.minusMonths(6);
                    to = today;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid period: " + period);
            }
        } else if (fromDate != null && toDate != null) {
            from = fromDate;
            to = toDate;
        } else {
            // Default to last 3 months
            from = today.minusMonths(3);
            to = today;
        }

        return new DateRange(from, to);
    }

    /**
     * Checks if cached data is stale (older than 1 hour).
     */
    private boolean isDataStale(DateRange dateRange) {
        PaymentBatchEntity latestBatch = paymentBatchRepository.findAll().stream()
                .filter(batch -> !batch.getCreatedAt().isAfter(dateRange.to().atTime(23, 59, 59)))
                .max((first, second) -> first.getCreatedAt().compareTo(second.getCreatedAt()))
                .orElse(null);
        if (latestBatch == null) {
            return true;
        }
        return ChronoUnit.HOURS.between(latestBatch.getUpdatedAt(), LocalDateTime.now()) > 1;
    }

    private record DateRange(LocalDate from, LocalDate to) {
    }

}
