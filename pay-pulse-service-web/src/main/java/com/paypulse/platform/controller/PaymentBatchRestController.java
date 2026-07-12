package com.paypulse.platform.controller;

import com.paypulse.platform.dto.web.request.PaymentBatchCreateRequest;
import com.paypulse.platform.dto.web.response.PaymentBatchCreateResponse;
import com.paypulse.platform.service.PaymentBatchService;

import java.net.URI;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentBatchRestController {

	private final PaymentBatchService paymentBatchService;

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

		PaymentBatchCreateResponse response = paymentBatchService.createBatch(request);

		log.info("Payment batch created successfully. BatchId: {}, Status: {}, IsDuplicate: {}",
				response.batchId(), response.status(), response.isDuplicate());

		return ResponseEntity
				.accepted()
				.location(URI.create("/api/v1/payment-batches/" + response.batchId()))
				.body(response);
	}

//	@GetMapping("/payment-batches")
//	public List<PaymentBatchSummaryResponse> listPaymentBatches(
//			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
//			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
//		return paymentBatchService.listBatches(fromDate, toDate);
//	}
//
//	@GetMapping("/payment-batches/{batchId}/status")
//	public PaymentBatchStatusResponse getBatchStatus(@PathVariable String batchId) {
//		return paymentBatchService.getBatchStatus(batchId);
//	}
}
