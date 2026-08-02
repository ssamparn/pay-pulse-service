package com.paypulse.platform.mapper;

import com.paypulse.platform.common.dto.BatchStatus;
import com.paypulse.platform.web.dto.response.PaymentBatchListResponse;
import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import com.paypulse.platform.persistence.entity.PaymentTransactionEntity;
import com.paypulse.platform.util.PaginationWindow;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class PaymentBatchListResponseMapper {

    public PaymentBatchListResponse.BatchItem toBatchItem(
            PaymentBatchEntity batch,
            List<PaymentTransactionEntity> paymentTransactions
    ) {
        long successfulCount = paymentTransactions.stream()
                .filter(payment -> payment.getStatus() == BatchStatus.COMPLETED)
                .count();

        long failedCount = paymentTransactions.stream()
                .filter(payment -> payment.getStatus() == BatchStatus.FAILED)
                .count();

        return new PaymentBatchListResponse.BatchItem(
                batch.getBatchId(),
                batch.getStatus(),
                batch.getTotalAmount(),
                batch.getCurrency(),
                batch.getPaymentMethod().toString(),
                paymentTransactions.size(),
                (int) successfulCount,
                (int) failedCount,
                batch.getCreatedAt(),
                batch.getUpdatedAt(),
                "/api/v1/batch-payment/" + batch.getBatchId() + "/status",
                "/api/v1/batch-payment/" + batch.getBatchId() + "/payments"
        );
    }

    public PaymentBatchListResponse.Pagination toPagination(
            PaginationWindow paginationWindow
    ) {
        return new PaymentBatchListResponse.Pagination(
                paginationWindow.currentPage(),
                paginationWindow.pageSize(),
                paginationWindow.totalPages(),
                paginationWindow.totalRecords(),
                paginationWindow.hasNextPage(),
                paginationWindow.hasPreviousPage()
        );
    }

    public PaymentBatchListResponse.Filters toFilters(String period, LocalDate fromDate, LocalDate toDate) {
        return new PaymentBatchListResponse.Filters(period, fromDate, toDate);
    }

    public PaymentBatchListResponse.Summary toSummary(List<PaymentBatchEntity> batches) {
        long totalBatches = batches.size();

        BigDecimal totalAmount = batches.stream()
                .map(PaymentBatchEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long completedBatches = batches.stream()
                .filter(batch -> batch.getStatus() == BatchStatus.COMPLETED)
                .count();

        long partiallyCompletedBatches = batches.stream()
                .filter(batch -> batch.getStatus() == BatchStatus.PARTIALLY_COMPLETED)
                .count();

        long failedBatches = batches.stream()
                .filter(batch -> batch.getStatus() == BatchStatus.FAILED)
                .count();

        return new PaymentBatchListResponse.Summary(
                totalBatches,
                totalAmount,
                completedBatches,
                partiallyCompletedBatches,
                failedBatches
        );
    }

    public PaymentBatchListResponse toResponse(
            List<PaymentBatchListResponse.BatchItem> batches,
            PaymentBatchListResponse.Pagination pagination,
            PaymentBatchListResponse.Filters filters,
            PaymentBatchListResponse.Summary summary
    ) {
        return new PaymentBatchListResponse(batches, pagination, filters, summary);
    }
}