package com.paypulse.platform.api;

import com.paypulse.platform.dto.web.request.PaymentBatchCreateRequest;
import com.paypulse.platform.dto.web.response.PaymentBatchCreateResponse;
import com.paypulse.platform.dto.web.response.PaymentBatchListResponse;
import com.paypulse.platform.dto.web.response.PaymentBatchStatusResponse;

import java.net.URI;
import java.time.LocalDate;

import com.paypulse.platform.service.BatchPaymentInitiationService;
import com.paypulse.platform.service.BatchPaymentStatusService;
import com.paypulse.platform.service.HistoricalBatchPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentBatchRestController {

	private final BatchPaymentInitiationService batchPaymentInitiationService;
	private final BatchPaymentStatusService batchPaymentStatusService;
	private final HistoricalBatchPaymentService historicalBatchPaymentService;

	/**
	 * Creates a new payment batch with the provided details.
	 * Validates the request, creates the batch, and initializes individual payments with PENDING status.
	 *
	 * @param request The payment batch creation request containing batch and payment details
	 * @return 202 Accepted with batch details and status tracking URL
	 */
	@PostMapping("/payment-batch")
	public ResponseEntity<PaymentBatchCreateResponse> createPaymentBatch(@Valid @RequestBody PaymentBatchCreateRequest request) {
		log.info("POST /api/v1/payment-batch - Creating batch payment request with batchId: {}", request.batchId());

		PaymentBatchCreateResponse response = batchPaymentInitiationService.createBatch(request);

		log.info("Payment batch created successfully. BatchId: {}, Status: {}, IsDuplicate: {}",
				response.batchId(), response.status(), response.isDuplicate());

		return ResponseEntity
				.accepted()
				.location(URI.create("/api/v1/payment-batches/" + response.batchId()))
				.body(response);
	}

	@GetMapping("/payment-batches/{batchId}/status")
	public PaymentBatchStatusResponse getBatchStatus(@PathVariable String batchId) {
		return batchPaymentStatusService.getBatchStatus(batchId);
	}

	@GetMapping("/payment-batches")
	public ResponseEntity<PaymentBatchListResponse> getHistoricalBatches(
			@RequestParam(required = false) String period,
			@RequestParam(required = false) LocalDate fromDate,
			@RequestParam(required = false) LocalDate toDate,
			@RequestParam(defaultValue = "1") Integer page,
			@RequestParam(defaultValue = "20") Integer pageSize) {

		log.info("GET /api/v1/payment-batches - Fetching historical batches. Period: {}, FromDate: {}, ToDate: {}, Page: {}, PageSize: {}",
				period, fromDate, toDate, page, pageSize);

		PaymentBatchListResponse response = historicalBatchPaymentService.getHistoricalBatches(period, fromDate, toDate, page, pageSize);

		return ResponseEntity.ok(response);
	}

}
