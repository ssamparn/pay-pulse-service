package com.paypulse.platform.infrastructure.soap.client.impl;

import com.paypulse.platform.infrastructure.soap.SoapBatchProcessingResult;
import com.paypulse.platform.infrastructure.soap.client.AbstractSpringWsSoapClient;
import com.paypulse.platform.infrastructure.soap.mapper.BatchSoapRequestMapper;
import com.paypulse.platform.infrastructure.soap.mapper.BatchSoapResponseMapper;
import com.paypulse.platform.infrastructure.soap.model.req.ProcessBatchPaymentReq;
import com.paypulse.platform.infrastructure.soap.model.rpy.ProcessBatchPaymentRpy;
import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import com.paypulse.platform.persistence.entity.PaymentTransactionEntity;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.oxm.Marshaller;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class BatchPaymentSoapClient extends AbstractSpringWsSoapClient<ProcessBatchPaymentReq, ProcessBatchPaymentRpy> {

	private final BatchSoapRequestMapper batchSoapRequestMapper;
	private final BatchSoapResponseMapper batchSoapResponseMapper;

	@Value("${paypulse.soap.batch-payment.uri:http://localhost:7070/ws}")
	private String batchPaymentSoapEndpoint;

	public BatchPaymentSoapClient(
			BatchSoapRequestMapper batchSoapRequestMapper,
			BatchSoapResponseMapper batchSoapResponseMapper,
			Marshaller batchSoapMarshaller
	) {
		super(batchSoapMarshaller);
		this.batchSoapRequestMapper = batchSoapRequestMapper;
		this.batchSoapResponseMapper = batchSoapResponseMapper;
	}

	@PostConstruct
	void initializeGateway() {
		setDefaultUri(batchPaymentSoapEndpoint);
	}

	public SoapBatchProcessingResult processBatch(PaymentBatchEntity batch, List<PaymentTransactionEntity> transactions) {
		ProcessBatchPaymentReq request = batchSoapRequestMapper.toProcessBatchPaymentReq(batch, transactions);
		ProcessBatchPaymentRpy response = send(request);
		log.info("SOAP gateway processed batchId={} with {} transactions", batch.getBatchId(), transactions.size());
		return batchSoapResponseMapper.toSoapBatchProcessingResult(response);
	}

	@Override
	protected ProcessBatchPaymentRpy mapResponse(Object response) {
		if (response instanceof ProcessBatchPaymentRpy processBatchPaymentRpy) {
			return processBatchPaymentRpy;
		}
		throw new IllegalStateException("Unexpected SOAP response type: " + response);
	}
}
