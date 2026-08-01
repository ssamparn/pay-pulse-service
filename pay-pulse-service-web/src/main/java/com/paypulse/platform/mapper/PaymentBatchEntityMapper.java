package com.paypulse.platform.mapper;

import com.paypulse.platform.dto.common.BatchStatus;
import com.paypulse.platform.dto.web.request.PaymentBatchCreateRequest;
import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PaymentBatchEntityMapper {

    public PaymentBatchEntity toPaymentBatchEntity(PaymentBatchCreateRequest request, String generatedBatchId, LocalDateTime acceptedAt) {
        int totalTransactions = request.payments().size();

        return PaymentBatchEntity.create()
                .batchId(generatedBatchId)
                .merchantId(request.merchantId())
                .customerId(request.customerId())
                .externalBatchId(request.batchId())
                .status(BatchStatus.PENDING)
                .totalAmount(request.totalAmount())
                .currency(request.currency())
                .paymentMethod(request.paymentMethod())
                .executionDate(request.executionDate())
                .batchDescription(request.batchDescription())
                .requestedBy(request.requestedBy())
                .paymentsCount(totalTransactions)
                .totalTransactions(totalTransactions)
                .successfulTransactions(0)
                .failedTransactions(0)
                .pendingTransactions(totalTransactions)
                .progressPercentage(0)
                .recoveryAttemptCount(0)
                .createdAt(acceptedAt)
                .updatedAt(acceptedAt)
                .build();
    }
}

