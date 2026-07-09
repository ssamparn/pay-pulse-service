package com.paypulse.platform.paypulsewebapi.controller;

import com.paypulse.platform.paypulsewebapi.dto.PaymentBatchCreateRequest;
import com.paypulse.platform.paypulsewebapi.dto.PaymentBatchCreateResponse;
import com.paypulse.platform.paypulsewebapi.dto.PaymentBatchStatusResponse;
import com.paypulse.platform.paypulsewebapi.dto.PaymentBatchSummaryResponse;
import com.paypulse.platform.paypulsewebapi.service.PaymentBatchService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PaymentBatchRestController {

	private final PaymentBatchService paymentBatchService;

	public PaymentBatchRestController(PaymentBatchService paymentBatchService) {
		this.paymentBatchService = paymentBatchService;
	}

	@PostMapping("/payment-batch")
	@ResponseStatus(HttpStatus.CREATED)
	public PaymentBatchCreateResponse createPaymentBatch(@RequestBody PaymentBatchCreateRequest request) {
		return paymentBatchService.createBatch(request);
	}

	@GetMapping("/payment-batches")
	public List<PaymentBatchSummaryResponse> listPaymentBatches(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
		return paymentBatchService.listBatches(fromDate, toDate);
	}

	@GetMapping("/payment-batches/{batchId}/status")
	public PaymentBatchStatusResponse getBatchStatus(@PathVariable String batchId) {
		return paymentBatchService.getBatchStatus(batchId);
	}
}
