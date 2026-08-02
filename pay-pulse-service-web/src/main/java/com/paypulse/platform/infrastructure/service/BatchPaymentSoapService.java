package com.paypulse.platform.infrastructure.service;

import com.paypulse.platform.infrastructure.soap.client.impl.BatchPaymentSoapClient;
import com.paypulse.platform.infrastructure.soap.SoapBatchProcessingResult;
import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import com.paypulse.platform.persistence.entity.PaymentTransactionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchPaymentSoapService {

	private final BatchPaymentSoapClient batchPaymentSoapClient;

	public SoapBatchProcessingResult submitBatch(PaymentBatchEntity batch, List<PaymentTransactionEntity> transactions) {
		log.debug("Invoking SOAP submitBatch for batchId={} with {} transactions", batch.getBatchId(), transactions.size());
		return batchPaymentSoapClient.submitBatch(batch, transactions);
	}
}
