package com.paypulse.platform.infrastructure.worker;

import com.paypulse.platform.dto.web.request.PaymentBatchCreateRequest;
import com.paypulse.platform.mapper.PaymentBatchEntityMapper;
import com.paypulse.platform.mapper.PaymentTransactionEntityMapper;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchPaymentProcessingWorker {

	private final PaymentBatchRepository paymentBatchRepository;
	private final PaymentTransactionRepository paymentTransactionRepository;
	private final IdempotencyService idempotencyService;
	private final PaymentBatchEntityMapper paymentBatchEntityMapper;
	private final PaymentTransactionEntityMapper paymentTransactionEntityMapper;

	@Async("batchPersistenceExecutor")
	@Transactional
	public void persistBatchAsync(PaymentBatchCreateRequest request, String generatedBatchId, LocalDateTime acceptedAt) {
		if (idempotencyService.getPersistedBatch(request.idempotencyKey()) != null) {
			log.info("Skipping async persistence because batch already exists for idempotencyKey={}", request.idempotencyKey());
			return;
		}

		PaymentBatchEntity paymentBatchEntity = paymentBatchEntityMapper.toPaymentBatchEntity(request, generatedBatchId, acceptedAt);

		try {
			PaymentBatchEntity savedBatch = paymentBatchRepository.save(paymentBatchEntity);

			List<PaymentTransactionEntity> paymentTransactions =
					paymentTransactionEntityMapper.toPaymentTransactionEntities(request, savedBatch.getBatchId(), acceptedAt);

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

}
