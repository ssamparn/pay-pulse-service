package com.paypulse.platform.mapper;

import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import com.paypulse.platform.dto.web.response.PaymentBatchStatusResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PaymentBatchStatusResponseMapper {

    public PaymentBatchStatusResponse toResponse(
            PaymentBatchEntity batch,
            Integer totalTransactions,
            Integer successfulTransactions,
            Integer failedTransactions,
            Integer pendingTransactions,
            Integer retryableFailures,
            Integer permanentFailures,
            String lastErrorMessage,
            LocalDateTime estimatedCompletionTime
    ) {
        String batchId = batch.getBatchId();

        PaymentBatchStatusResponse.Summary summary = new PaymentBatchStatusResponse.Summary(
                totalTransactions,
                successfulTransactions,
                failedTransactions,
                pendingTransactions
        );

        PaymentBatchStatusResponse.Timing timing = new PaymentBatchStatusResponse.Timing(
                batch.getCreatedAt(),
                batch.getUpdatedAt(),
                estimatedCompletionTime
        );

        PaymentBatchStatusResponse.FailureInfo failureInfo = new PaymentBatchStatusResponse.FailureInfo(
                retryableFailures,
                permanentFailures,
                lastErrorMessage
        );

        PaymentBatchStatusResponse.Links links = new PaymentBatchStatusResponse.Links(
                "/api/v1/batch-payment/" + batchId + "/payments",
                "/api/v1/batch-payment/" + batchId + "/payments?status=FAILED"
        );

        return new PaymentBatchStatusResponse(
                batchId,
                batch.getStatus(),
                summary,
                timing,
                failureInfo,
                links
        );
    }
}