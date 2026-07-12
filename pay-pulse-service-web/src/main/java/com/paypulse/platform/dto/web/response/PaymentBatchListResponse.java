package com.paypulse.platform.dto.web.response;

import com.paypulse.platform.dto.common.BatchStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentBatchListResponse(
        List<BatchItem> batches,
        Pagination pagination,
        Filters filters,
        Summary summary
) {
    public record BatchItem(
            String batchId,
            String externalBatchId,
            BatchStatus status,
            BigDecimal totalAmount,
            String currency,
            String paymentMethod,
            Integer paymentCount,
            Integer successfulPayments,
            Integer failedPayments,
            LocalDateTime createdAt,
            LocalDateTime completedAt,
            String statusUrl,
            String detailsUrl
    ) {
    }

    public record Pagination(
            Integer currentPage,
            Integer pageSize,
            Integer totalPages,
            Long totalRecords,
            Boolean hasNextPage,
            Boolean hasPreviousPage
    ) {
    }

    public record Filters(
            String period,
            LocalDate fromDate,
            LocalDate toDate
    ) {
    }

    public record Summary(
            Long totalBatches,
            BigDecimal totalAmount,
            Long completedBatches,
            Long partiallyCompletedBatches,
            Long failedBatches
    ) {
    }
}
