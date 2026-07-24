package com.paypulse.platform.infrastructure.worker;

import com.paypulse.platform.dto.common.BatchStatus;
import com.paypulse.platform.config.BatchProcessingSchedulerProperties;
import com.paypulse.platform.infrastructure.service.BatchPaymentSoapService;
import com.paypulse.platform.infrastructure.soap.SoapBatchProcessingResult;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchPaymentProcessingWorker {

	private final PaymentBatchRepository paymentBatchRepository;
	private final PaymentTransactionRepository paymentTransactionRepository;
	private final IdempotencyService idempotencyService;
	private final PaymentBatchEntityMapper paymentBatchEntityMapper;
	private final PaymentTransactionEntityMapper paymentTransactionEntityMapper;
	private final BatchPaymentSoapService batchPaymentSoapService;
	private final BatchProcessingSchedulerProperties schedulerProperties;

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

	@Async("batchProcessingExecutor")
	@Transactional
	public void processClaimedBatchAsync(String batchId) {
		PaymentBatchEntity batch = paymentBatchRepository.findById(batchId)
				.orElseThrow(() -> new IllegalStateException("Claimed batch not found: " + batchId));

		List<PaymentTransactionEntity> transactions = paymentTransactionRepository.findByBatchId(batchId);
		if (transactions.isEmpty()) {
			log.warn("No transactions found for claimed batchId={}; moving batch to FAILED", batchId);
			markBatchAsFailed(batch, "No transactions found for claimed batch", LocalDateTime.now());
			return;
		}

		LocalDateTime processingStartedAt = LocalDateTime.now();
		markTransactionsAsProcessing(transactions, processingStartedAt);

		try {
			SoapBatchProcessingResult soapResult = batchPaymentSoapService.submitBatch(batchId, transactions);
			applySoapOutcome(batch, transactions, soapResult, LocalDateTime.now());
		} catch (RuntimeException exception) {
			handleSoapProcessingFailure(batch, transactions, processingStartedAt, exception);
		}
	}

	private void handleSoapProcessingFailure(
			PaymentBatchEntity batch,
			List<PaymentTransactionEntity> transactions,
			LocalDateTime failedAt,
			RuntimeException exception
	) {
		int currentAttempts = batch.getRecoveryAttemptCount() == null ? 0 : batch.getRecoveryAttemptCount();
		int nextAttempt = currentAttempts + 1;
		batch.setRecoveryAttemptCount(nextAttempt);

		if (nextAttempt >= schedulerProperties.getMaxRecoveryAttempts()) {
			markRemainingTransactionsAsFailed(transactions, failedAt, "Max retry attempts reached: " + nextAttempt);
			paymentTransactionRepository.saveAll(transactions);
			updateBatchAggregates(batch, transactions, failedAt);
			log.error(
					"SOAP processing failed permanently for batchId={} after {} attempts. Marked batch as terminal status={}",
					batch.getBatchId(),
					nextAttempt,
					batch.getStatus(),
					exception
			);
			return;
		}

		revertBatchToPending(batch, transactions, failedAt);
		log.error(
				"SOAP processing failed for batchId={}; batch queued for retry attempt {}/{}",
				batch.getBatchId(),
				nextAttempt,
				schedulerProperties.getMaxRecoveryAttempts(),
				exception
		);
	}

	private void markRemainingTransactionsAsFailed(
			List<PaymentTransactionEntity> transactions,
			LocalDateTime failedAt,
			String failureReason
	) {
		for (PaymentTransactionEntity transaction : transactions) {
			if (transaction.getStatus() == BatchStatus.COMPLETED || transaction.getStatus() == BatchStatus.FAILED) {
				continue;
			}
			transaction.setStatus(BatchStatus.FAILED);
			transaction.setFailureReason(failureReason);
			transaction.setRetryable(false);
			transaction.setProcessedAt(failedAt);
			transaction.setUpdatedAt(failedAt);
		}
	}

	private void markTransactionsAsProcessing(List<PaymentTransactionEntity> transactions, LocalDateTime processingStartedAt) {
		for (PaymentTransactionEntity transaction : transactions) {
			transaction.setStatus(BatchStatus.PROCESSING);
			transaction.setUpdatedAt(processingStartedAt);
			transaction.setFailureReason(null);
			transaction.setRetryable(false);
		}
		paymentTransactionRepository.saveAll(transactions);
	}

	private void applySoapOutcome(
			PaymentBatchEntity batch,
			List<PaymentTransactionEntity> transactions,
			SoapBatchProcessingResult soapResult,
			LocalDateTime processedAt
	) {
		Map<String, SoapBatchProcessingResult.SoapTransactionResult> resultByExternalPaymentId = new HashMap<>();
		for (SoapBatchProcessingResult.SoapTransactionResult result : soapResult.transactions()) {
			resultByExternalPaymentId.put(result.externalPaymentId(), result);
		}

		for (PaymentTransactionEntity transaction : transactions) {
			SoapBatchProcessingResult.SoapTransactionResult result = resultByExternalPaymentId.get(transaction.getExternalPaymentId());
			if (result == null) {
				transaction.setStatus(BatchStatus.FAILED);
				transaction.setFailureReason("Missing SOAP outcome for transaction");
				transaction.setRetryable(true);
				transaction.setProcessedAt(processedAt);
				transaction.setUpdatedAt(processedAt);
				continue;
			}

			if (result.outcome() == SoapBatchProcessingResult.TransactionOutcome.SUCCESS) {
				transaction.setStatus(BatchStatus.COMPLETED);
				transaction.setFailureReason(null);
				transaction.setRetryable(false);
			} else {
				transaction.setStatus(BatchStatus.FAILED);
				transaction.setFailureReason(result.failureReason());
				transaction.setRetryable(result.retryable());
			}

			LocalDateTime transactionProcessedAt = result.processedAt() == null ? processedAt : result.processedAt();
			transaction.setProcessedAt(transactionProcessedAt);
			transaction.setUpdatedAt(processedAt);
		}

		paymentTransactionRepository.saveAll(transactions);
		updateBatchAggregates(batch, transactions, processedAt);
		log.info("Completed SOAP processing for batchId={} with final status={}", batch.getBatchId(), batch.getStatus());
	}

	private void updateBatchAggregates(PaymentBatchEntity batch, List<PaymentTransactionEntity> transactions, LocalDateTime updatedAt) {
		int totalTransactions = transactions.size();
		int successfulTransactions = (int) transactions.stream().filter(tx -> tx.getStatus() == BatchStatus.COMPLETED).count();
		int failedTransactions = (int) transactions.stream().filter(tx -> tx.getStatus() == BatchStatus.FAILED).count();
		int pendingTransactions = (int) transactions.stream()
				.filter(tx -> tx.getStatus() == BatchStatus.PENDING || tx.getStatus() == BatchStatus.PROCESSING)
				.count();

		BatchStatus finalStatus = deriveBatchStatus(totalTransactions, successfulTransactions, failedTransactions, pendingTransactions);
		int completedTransactions = successfulTransactions + failedTransactions;
		int progressPercentage = totalTransactions == 0 ? 0 : (int) ((completedTransactions * 100.0) / totalTransactions);

		batch.setStatus(finalStatus);
		batch.setTotalTransactions(totalTransactions);
		batch.setPaymentsCount(totalTransactions);
		batch.setSuccessfulTransactions(successfulTransactions);
		batch.setFailedTransactions(failedTransactions);
		batch.setPendingTransactions(pendingTransactions);
		batch.setProgressPercentage(progressPercentage);
		batch.setUpdatedAt(updatedAt);
		batch.setCompletedAt(isTerminalStatus(finalStatus) ? updatedAt : null);

		paymentBatchRepository.save(batch);
	}

	private void markBatchAsFailed(PaymentBatchEntity batch, String reason, LocalDateTime failedAt) {
		batch.setStatus(BatchStatus.FAILED);
		batch.setSuccessfulTransactions(0);
		batch.setFailedTransactions(batch.getTotalTransactions());
		batch.setPendingTransactions(0);
		batch.setProgressPercentage(100);
		batch.setCompletedAt(failedAt);
		batch.setUpdatedAt(failedAt);
		paymentBatchRepository.save(batch);
		log.warn("Batch {} marked as FAILED. Reason={}", batch.getBatchId(), reason);
	}

	private void revertBatchToPending(
			PaymentBatchEntity batch,
			List<PaymentTransactionEntity> transactions,
			LocalDateTime updatedAt
	) {
		for (PaymentTransactionEntity transaction : transactions) {
			if (transaction.getStatus() == BatchStatus.PROCESSING) {
				transaction.setStatus(BatchStatus.PENDING);
				transaction.setUpdatedAt(updatedAt);
			}
		}
		paymentTransactionRepository.saveAll(transactions);
		updateBatchAggregates(batch, transactions, updatedAt);
	}

	private BatchStatus deriveBatchStatus(
			int totalTransactions,
			int successfulTransactions,
			int failedTransactions,
			int pendingTransactions
	) {
		if (pendingTransactions == totalTransactions) {
			return BatchStatus.PENDING;
		}
		if (pendingTransactions > 0) {
			return BatchStatus.PROCESSING;
		}
		if (successfulTransactions == totalTransactions) {
			return BatchStatus.COMPLETED;
		}
		if (failedTransactions == totalTransactions) {
			return BatchStatus.FAILED;
		}
		return BatchStatus.PARTIALLY_COMPLETED;
	}

	private boolean isTerminalStatus(BatchStatus status) {
		return status == BatchStatus.COMPLETED
				|| status == BatchStatus.FAILED
				|| status == BatchStatus.PARTIALLY_COMPLETED;
	}
}
