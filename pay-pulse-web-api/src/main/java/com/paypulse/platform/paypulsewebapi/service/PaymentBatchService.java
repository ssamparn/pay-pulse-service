package com.paypulse.platform.paypulsewebapi.service;

import com.paypulse.platform.paypulsewebapi.dto.PaymentBatchCreateRequest;
import com.paypulse.platform.paypulsewebapi.dto.PaymentBatchCreateResponse;
import com.paypulse.platform.paypulsewebapi.dto.PaymentBatchStatusResponse;
import com.paypulse.platform.paypulsewebapi.dto.PaymentBatchSummaryResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentBatchService {

    private final Map<String, PaymentBatch> batches = new ConcurrentHashMap<>();

    public PaymentBatchCreateResponse createBatch(PaymentBatchCreateRequest request) {
        validateCreateRequest(request);

        String batchId = UUID.randomUUID().toString();
        PaymentBatch batch = new PaymentBatch(
                batchId,
                request.clientReference().trim(),
                "RECEIVED",
                request.paymentCount(),
                request.totalAmount(),
                request.currency().trim().toUpperCase(),
                Instant.now()
        );

        batches.put(batchId, batch);
        return new PaymentBatchCreateResponse(batch.batchId(), batch.status(), batch.createdAt());
    }

    public PaymentBatchStatusResponse getBatchStatus(String batchId) {
        PaymentBatch batch = getRequiredBatch(batchId);
        return new PaymentBatchStatusResponse(
                batch.batchId(),
                batch.clientReference(),
                batch.status(),
                batch.paymentCount(),
                batch.totalAmount(),
                batch.currency(),
                batch.createdAt()
        );
    }

    public List<PaymentBatchSummaryResponse> listBatches(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "fromDate and toDate are required and fromDate must be on/before toDate");
        }

        return batches.values().stream()
                .filter(batch -> {
                    LocalDate createdDate = batch.createdAt().atZone(ZoneOffset.UTC).toLocalDate();
                    return !createdDate.isBefore(fromDate) && !createdDate.isAfter(toDate);
                })
                .sorted(Comparator.comparing(PaymentBatch::createdAt).reversed())
                .map(batch -> new PaymentBatchSummaryResponse(
                        batch.batchId(),
                        batch.status(),
                        batch.paymentCount(),
                        batch.totalAmount(),
                        batch.currency(),
                        batch.createdAt()
                ))
                .toList();
    }

    private PaymentBatch getRequiredBatch(String batchId) {
        if (batchId == null || batchId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "batchId is required");
        }

        PaymentBatch batch = batches.get(batchId);
        if (batch == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "batch not found");
        }
        return batch;
    }

    private void validateCreateRequest(PaymentBatchCreateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }
        if (request.clientReference() == null || request.clientReference().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "clientReference is required");
        }
        if (request.paymentCount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentCount must be > 0");
        }
        if (request.totalAmount() == null || request.totalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "totalAmount must be > 0");
        }
        if (request.currency() == null || request.currency().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "currency is required");
        }
    }

    private record PaymentBatch(
            String batchId,
            String clientReference,
            String status,
            int paymentCount,
            BigDecimal totalAmount,
            String currency,
            Instant createdAt
    ) {
    }
}

