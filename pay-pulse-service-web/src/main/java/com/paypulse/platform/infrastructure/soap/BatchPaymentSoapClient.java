package com.paypulse.platform.infrastructure.soap;

import com.paypulse.platform.persistence.entity.PaymentTransactionEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class BatchPaymentSoapClient {

	public SoapBatchProcessingResult submitBatch(String batchId, List<PaymentTransactionEntity> transactions) {
		List<SoapBatchProcessingResult.SoapTransactionResult> results = transactions.stream()
				.map(this::simulateTransactionResult)
				.toList();

		log.info("SOAP stub processed batchId={} with {} transactions", batchId, results.size());
		return new SoapBatchProcessingResult(batchId, results, LocalDateTime.now());
	}

	private SoapBatchProcessingResult.SoapTransactionResult simulateTransactionResult(PaymentTransactionEntity transaction) {
		LocalDateTime processedAt = LocalDateTime.now();
		boolean success = ThreadLocalRandom.current().nextInt(100) < 85;

		if (success) {
			return new SoapBatchProcessingResult.SoapTransactionResult(
					transaction.getExternalPaymentId(),
					SoapBatchProcessingResult.TransactionOutcome.SUCCESS,
					false,
					null,
					processedAt
			);
		}

		boolean retryable = ThreadLocalRandom.current().nextBoolean();
		String failureReason = retryable ? "Temporary upstream timeout" : "IBAN validation failed";

		return new SoapBatchProcessingResult.SoapTransactionResult(
				transaction.getExternalPaymentId(),
				SoapBatchProcessingResult.TransactionOutcome.FAILED,
				retryable,
				failureReason,
				processedAt
		);
	}
}
