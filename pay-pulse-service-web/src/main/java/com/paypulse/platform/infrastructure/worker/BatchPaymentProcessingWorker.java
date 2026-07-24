package com.paypulse.platform.infrastructure.worker;

import com.paypulse.platform.dto.common.BatchStatus;
import com.paypulse.platform.dto.web.request.PaymentBatchCreateRequest;
import com.paypulse.platform.persistence.entity.PaymentBatchEntity;
import com.paypulse.platform.persistence.entity.PaymentTransactionEntity;
import com.paypulse.platform.persistence.repository.PaymentBatchRepository;
import com.paypulse.platform.persistence.repository.PaymentTransactionRepository;
import com.paypulse.platform.persistence.service.IdempotencyService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchPaymentProcessingWorker {

	private final PaymentBatchRepository paymentBatchRepository;
	private final PaymentTransactionRepository paymentTransactionRepository;
	private final IdempotencyService idempotencyService;

	@Async("batchPersistenceExecutor")
	@Transactional
	public void persistBatchAsync(PaymentBatchCreateRequest request, String generatedBatchId, LocalDateTime acceptedAt) {
		if (idempotencyService.getPersistedBatch(request.idempotencyKey()) != null) {
			log.info("Skipping async persistence because batch already exists for idempotencyKey={}", request.idempotencyKey());
			return;
		}

		int totalTransactions = request.payments().size();

		PaymentBatchEntity paymentBatchEntity = PaymentBatchEntity.create()
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
				.idempotencyKey(request.idempotencyKey())
				.paymentsCount(totalTransactions)
				.totalTransactions(totalTransactions)
				.successfulTransactions(0)
				.failedTransactions(0)
				.pendingTransactions(totalTransactions)
				.progressPercentage(0)
				.createdAt(acceptedAt)
				.updatedAt(acceptedAt)
				.build();

		try {
			PaymentBatchEntity savedBatch = paymentBatchRepository.save(paymentBatchEntity);

			List<PaymentTransactionEntity> paymentTransactions = request.payments().stream()
					.map(paymentItem -> PaymentTransactionEntity.create()
							.paymentId(generatePaymentId())
							.externalPaymentId(paymentItem.paymentId())
							.batchId(savedBatch.getBatchId())
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

			paymentTransactionRepository.saveAll(paymentTransactions);
			idempotencyService.storeIdempotencyMapping(request.idempotencyKey(), savedBatch.getBatchId());

			log.info("Persisted batchId={} with {} pending transactions in background",
					savedBatch.getBatchId(), paymentTransactions.size());
		} catch (DataIntegrityViolationException exception) {
			log.warn("Ignoring duplicate async persistence attempt for idempotencyKey={} / externalBatchId={}",
					request.idempotencyKey(), request.batchId(), exception);
			idempotencyService.clearPendingSubmission(request.idempotencyKey());
		} catch (RuntimeException exception) {
			idempotencyService.clearPendingSubmission(request.idempotencyKey());
			log.error("Failed to persist batchId={} asynchronously", generatedBatchId, exception);
			throw exception;
		}
	}

	private String generatePaymentId() {
		return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
	}
}
