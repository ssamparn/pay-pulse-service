package com.paypulse.platform.infrastructure.soap.mapper;

import com.paypulse.platform.infrastructure.soap.model.req.SubmitBatchReq;
import com.paypulse.platform.infrastructure.soap.model.req.SubmitPaymentTxnReq;
import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import com.paypulse.platform.persistence.entity.PaymentTransactionEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BatchSoapRequestMapper {

    public SubmitBatchReq toSubmitBatchReq(PaymentBatchEntity batch, List<PaymentTransactionEntity> transactions) {
        SubmitBatchReq request = new SubmitBatchReq();
        request.setBatchId(batch.getBatchId());
        request.setExternalBatchId(batch.getExternalBatchId());
        request.setMerchantId(batch.getMerchantId());
        request.setCustomerId(batch.getCustomerId());
        request.setIdempotencyKey(batch.getIdempotencyKey());
        request.setPaymentMethod(batch.getPaymentMethod().name());
        request.setExecutionDate(batch.getExecutionDate().toString());
        request.setTotalAmount(batch.getTotalAmount().toPlainString());
        request.setCurrency(batch.getCurrency());
        request.setRequestedBy(batch.getRequestedBy());

        List<SubmitPaymentTxnReq> transactionRequests = transactions.stream()
                .map(this::toSubmitPaymentTxnReq)
                .toList();
        request.setTransactions(transactionRequests);
        return request;
    }

    private SubmitPaymentTxnReq toSubmitPaymentTxnReq(PaymentTransactionEntity transaction) {
        SubmitPaymentTxnReq request = new SubmitPaymentTxnReq();
        request.setPaymentId(transaction.getPaymentId());
        request.setExternalPaymentId(transaction.getExternalPaymentId());
        request.setBeneficiaryId(transaction.getBeneficiaryId());
        request.setBeneficiaryName(transaction.getBeneficiaryName());
        request.setBeneficiaryIban(transaction.getBeneficiaryIBAN());
        request.setAmount(transaction.getAmount().toPlainString());
        request.setCurrency(transaction.getCurrency());
        request.setPaymentReference(transaction.getPaymentReference());
        return request;
    }
}

