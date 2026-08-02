package com.paypulse.platform.persistence.mapper;

import com.paypulse.platform.dto.common.BatchStatus;
import com.paypulse.platform.dto.web.request.PaymentBatchCreateRequest;
import com.paypulse.platform.persistence.entity.PaymentTransactionEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class PaymentTransactionEntityMapper {

    public List<PaymentTransactionEntity> toPaymentTransactionEntities(PaymentBatchCreateRequest request, String batchId, LocalDateTime acceptedAt) {
        return request.payments().stream()
                .map(paymentItem -> PaymentTransactionEntity.create()
                        .paymentId(generatePaymentId())
                        .externalPaymentId(paymentItem.paymentId())
                        .batchId(batchId)
                        .beneficiaryId(paymentItem.beneficiaryId())
                        .beneficiaryName(paymentItem.beneficiaryName())
                        .beneficiaryIBAN(paymentItem.beneficiaryIBAN())
                        .amount(paymentItem.amount())
                        .currency(request.currency())
                        .paymentReference(paymentItem.paymentReference())
                        .status(BatchStatus.PENDING)
                        .createdAt(acceptedAt)
                        .updatedAt(acceptedAt)
                        .build())
                .toList();
    }

    private String generatePaymentId() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

