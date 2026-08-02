package com.paypulse.platform.infrastructure.service;

import com.paypulse.platform.common.properties.BatchProcessingSchedulerProperties;
import com.paypulse.platform.common.dto.BatchStatus;
import com.paypulse.platform.infrastructure.worker.BatchPaymentProcessingWorker;
import com.paypulse.platform.persistence.repository.PaymentBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PendingBatchDispatchService {

    private final PaymentBatchRepository paymentBatchRepository;
    private final BatchPaymentProcessingWorker batchPaymentProcessingWorker;
    private final BatchProcessingSchedulerProperties schedulerProperties;

    public int dispatchPendingBatches() {
        List<String> candidateBatchIds = paymentBatchRepository.findBatchIdsByStatusOrderByCreatedAtAsc(
                BatchStatus.PENDING,
                PageRequest.of(0, schedulerProperties.getBatchSize())
        );

        if (candidateBatchIds.isEmpty()) {
            return 0;
        }

        int dispatchedCount = 0;
        LocalDateTime claimedAt = LocalDateTime.now();

        for (String batchId : candidateBatchIds) {
            int updatedRows = paymentBatchRepository.updateBatchStatusIfCurrentStatusMatches(
                    batchId,
                    BatchStatus.PENDING,
                    BatchStatus.PROCESSING,
                    claimedAt
            );

            if (updatedRows == 1) {
                dispatchedCount++;
                batchPaymentProcessingWorker.processClaimedBatchAsync(batchId);
            }
        }

        log.info("Dispatched {} claimed pending batches out of {} candidates", dispatchedCount, candidateBatchIds.size());
        return dispatchedCount;
    }
}

