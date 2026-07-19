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
import org.springframework.http.HttpStatus;
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
	 * Initiates a new batch payment with the provided details.
	 * Validates the request, creates the batch, and initializes individual payments with PENDING status.
	 *
	 * @param request The payment batch creation request containing batch and payment details
	 * @return 202 Accepted with batch details and status tracking URL
	 */
	@PostMapping("/batch-payment")
	public ResponseEntity<PaymentBatchCreateResponse> createPaymentBatch(@Valid @RequestBody PaymentBatchCreateRequest request) {
		log.info("POST /api/v1/batch-payment - Creating batch payment request with batchId: {}", request.batchId());

		PaymentBatchCreateResponse response = batchPaymentInitiationService.createBatch(request);

		log.info("Payment batch created successfully. BatchId: {}, Status: {}, IsDuplicate: {}",
				response.batchId(), response.status(), response.isDuplicate());

		return ResponseEntity
				.accepted()
				.location(URI.create("/api/v1/batch-payment/" + response.batchId() + "/status"))
				.body(response);
	}

	@GetMapping("/batch-payment/{batchId}/status")
	public ResponseEntity<PaymentBatchStatusResponse> getBatchStatus(@PathVariable String batchId) {
		PaymentBatchStatusResponse response = batchPaymentStatusService.getBatchStatus(batchId);
		log.info("Payment batch status response: {}", response);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping("/batch-payment")
	public ResponseEntity<PaymentBatchListResponse> getHistoricalBatches(
			@RequestParam(required = false) String period,
			@RequestParam(required = false) LocalDate fromDate,
			@RequestParam(required = false) LocalDate toDate,
			@RequestParam(defaultValue = "1") Integer page,
			@RequestParam(defaultValue = "20") Integer pageSize) {

		log.info("GET /api/v1/batch-payment - Fetching historical batches. Period: {}, FromDate: {}, ToDate: {}, Page: {}, PageSize: {}",
				period, fromDate, toDate, page, pageSize);

		PaymentBatchListResponse response = historicalBatchPaymentService.getHistoricalBatches(period, fromDate, toDate, page, pageSize);

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
