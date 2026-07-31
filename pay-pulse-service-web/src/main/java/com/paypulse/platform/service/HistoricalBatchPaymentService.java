package com.paypulse.platform.service;

import com.paypulse.platform.dto.web.response.PaymentBatchListResponse;
import com.paypulse.platform.infrastructure.service.HistoricalBatchSoapService;
import com.paypulse.platform.infrastructure.soap.HistoricalSoapBatchSnapshot;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricalBatchPaymentService {

    private final PaymentBatchRepository paymentBatchRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final HistoricalBatchSoapService historicalBatchSoapService;
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
    @Transactional
    public PaymentBatchListResponse getHistoricalBatches(
            String period, LocalDate fromDate, LocalDate toDate,
            Integer page, Integer pageSize) {

        log.debug("Retrieving historical batches. Period: {}, FromDate: {}, ToDate: {}", period, fromDate, toDate);

        // Step 1: Parse and validate filter parameters
        HistoricalDateRange dateRange = historicalDateRangeResolver.resolve(period, fromDate, toDate);
        log.debug("Calculated date range: {} to {}", dateRange.from(), dateRange.to());

        // Step 2: Check local database for cached batches
        List<PaymentBatchEntity> localBatches = loadLocalBatches(dateRange);
        log.debug("Found {} batches in local database", localBatches.size());

        // Step 3: If insufficient data, call SOAP historical API and cache it.
        if (localBatches.isEmpty() || historicalDataStalenessCalculator.isStale(localBatches, dateRange)) {
            syncFromSoap(period, dateRange, page, pageSize);
            localBatches = loadLocalBatches(dateRange);
            log.info("Post-SOAP-sync local historical batch count: {}", localBatches.size());
        }

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

    private List<PaymentBatchEntity> loadLocalBatches(HistoricalDateRange dateRange) {
        return new ArrayList<>(paymentBatchRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                dateRange.from().atStartOfDay(),
                dateRange.to().atTime(LocalTime.MAX)
        ));
    }

    private void syncFromSoap(String period, HistoricalDateRange dateRange, Integer page, Integer pageSize) {
        log.info("Local historical cache is empty/stale, syncing from SOAP. Period: {} to {}", dateRange.from(), dateRange.to());

        try {
            List<HistoricalSoapBatchSnapshot> snapshots = historicalBatchSoapService.fetchHistoricalBatches(
                    dateRange.from(),
                    dateRange.to(),
                    period,
                    page,
                    pageSize,
                    true
            );

            for (HistoricalSoapBatchSnapshot snapshot : snapshots) {
                persistSnapshot(snapshot);
            }

            log.info("Historical SOAP sync completed. Imported/updated {} batches", snapshots.size());
        } catch (Exception exception) {
            log.warn("Historical SOAP sync failed; serving local cache only. Reason: {}", exception.getMessage());
        }
    }

    private void persistSnapshot(HistoricalSoapBatchSnapshot snapshot) {
        PaymentBatchEntity batch = snapshot.batch();
        List<PaymentTransactionEntity> transactions = snapshot.transactions();

        // Ensure batch transaction counters remain consistent with imported transaction payload.
        if (!transactions.isEmpty()) {
            int total = transactions.size();
            int successful = (int) transactions.stream().filter(tx -> tx.getStatus() == com.paypulse.platform.dto.common.BatchStatus.COMPLETED).count();
            int failed = (int) transactions.stream().filter(tx -> tx.getStatus() == com.paypulse.platform.dto.common.BatchStatus.FAILED).count();
            int pending = Math.max(total - successful - failed, 0);

            batch.setPaymentsCount(total);
            batch.setTotalTransactions(total);
            batch.setSuccessfulTransactions(successful);
            batch.setFailedTransactions(failed);
            batch.setPendingTransactions(pending);
            batch.setProgressPercentage(total == 0 ? 0 : (int) Math.round((successful * 100.0d) / total));
            if (batch.getUpdatedAt() == null) {
                batch.setUpdatedAt(LocalDateTime.now());
            }
        }

        paymentBatchRepository.save(batch);

        // Replace batch transactions atomically for idempotent refresh from historical source.
        paymentTransactionRepository.deleteByBatchId(batch.getBatchId());
        if (!transactions.isEmpty()) {
            paymentTransactionRepository.saveAll(transactions);
        }
    }

    /**
     * Converts PaymentBatch entity to BatchItem DTO.
     */
    private PaymentBatchListResponse.BatchItem mapToBatchItem(PaymentBatchEntity batch) {
        List<PaymentTransactionEntity> paymentEntities = paymentTransactionRepository.findByBatchId(batch.getBatchId());
        return paymentBatchListResponseMapper.toBatchItem(batch, paymentEntities);
    }
}
