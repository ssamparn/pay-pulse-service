package com.paypulse.platform.service;

import com.paypulse.platform.dto.web.response.PaymentBatchListResponse;
import com.paypulse.platform.mapper.PaymentBatchListResponseMapper;
import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import com.paypulse.platform.persistence.entity.PaymentTransactionEntity;
import com.paypulse.platform.persistence.repository.PaymentBatchRepository;
import com.paypulse.platform.persistence.repository.PaymentTransactionRepository;
import com.paypulse.platform.util.PaginationWindow;
import com.paypulse.platform.util.Paginator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricalBatchPaymentService {

    private final PaymentBatchRepository paymentBatchRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentBatchListResponseMapper paymentBatchListResponseMapper;
    private final HistoricalDateRangeResolver historicalDateRangeResolver;
    private final HistoricalDataStalenessCalculator historicalDataStalenessCalculator;
    private final Paginator paginator;

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
        HistoricalDateRange dateRange = historicalDateRangeResolver.resolve(period, fromDate, toDate);
        log.debug("Calculated date range: {} to {}", dateRange.from(), dateRange.to());

        // Step 2: Check local database for cached batches
        List<PaymentBatchEntity> localBatches = new ArrayList<>(paymentBatchRepository.findAll().stream()
                .filter(batch -> !batch.getCreatedAt().isBefore(dateRange.from().atStartOfDay()))
                .filter(batch -> !batch.getCreatedAt().isAfter(dateRange.to().atTime(23, 59, 59)))
                .toList());
        log.debug("Found {} batches in local database", localBatches.size());

        // Step 3: If insufficient data, call SOAP historical API
        // SOAP integration is not wired yet, so rely on locally available data for now.
        if (localBatches.isEmpty() || historicalDataStalenessCalculator.isStale(localBatches, dateRange)) {
            log.info("Historical SOAP sync is skipped; using local cache for period: {} to {}",
                    dateRange.from(), dateRange.to());
        }

        // Sort by created date descending
        localBatches.sort((b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt()));

        int totalRecords = localBatches.size();
        PaginationWindow paginationWindow = paginator.paginate(totalRecords, page, pageSize);

        List<PaymentBatchEntity> paginatedBatches = localBatches.subList(
                paginationWindow.fromIndex(),
                paginationWindow.toIndex()
        );

        // Build batch items with summary stats
        List<PaymentBatchListResponse.BatchItem> batchItems = paginatedBatches.stream()
                .map(this::mapToBatchItem)
                .toList();

        PaymentBatchListResponse.Summary summary = paymentBatchListResponseMapper.toSummary(localBatches);

        PaymentBatchListResponse.Pagination pagination = paymentBatchListResponseMapper.toPagination(paginationWindow);

        PaymentBatchListResponse.Filters filters = paymentBatchListResponseMapper.toFilters(
                period,
                dateRange.from(),
                dateRange.to()
        );

        PaymentBatchListResponse response = paymentBatchListResponseMapper.toResponse(
                batchItems,
                pagination,
                filters,
                summary
        );

        log.info("Historical batches retrieved. Total: {}, Page: {}/{}, Period: {} to {}",
                totalRecords,
                paginationWindow.currentPage(),
                paginationWindow.totalPages(),
                dateRange.from(),
                dateRange.to());

        return response;
    }

    /**
     * Converts PaymentBatch entity to BatchItem DTO.
     */
    private PaymentBatchListResponse.BatchItem mapToBatchItem(PaymentBatchEntity batch) {
        List<PaymentTransactionEntity> paymentEntities = paymentTransactionRepository.findByBatchId(batch.getBatchId());
        return paymentBatchListResponseMapper.toBatchItem(batch, paymentEntities);
    }
}
