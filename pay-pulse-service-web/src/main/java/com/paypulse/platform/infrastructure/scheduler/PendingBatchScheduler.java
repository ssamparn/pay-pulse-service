package com.paypulse.platform.infrastructure.scheduler;

import com.paypulse.platform.infrastructure.service.ProcessingBatchRecoveryService;
import com.paypulse.platform.infrastructure.service.PendingBatchDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingBatchScheduler {

    private final PendingBatchDispatchService pendingBatchDispatchService;
    private final ProcessingBatchRecoveryService processingBatchRecoveryService;

    @Scheduled(
            fixedDelayString = "${paypulse.processing.scheduler.fixed-delay-millis:5000}",
            initialDelayString = "${paypulse.processing.scheduler.initial-delay-millis:10000}"
    )
    public void schedulePendingBatchProcessing() {
        try {
            int dispatchedCount = pendingBatchDispatchService.dispatchPendingBatches();
            if (dispatchedCount > 0) {
                log.info("Scheduler dispatched {} pending batches for asynchronous processing", dispatchedCount);
            }
        } catch (Exception exception) {
            log.error("Pending batch scheduler failed", exception);
        }
    }

    @Scheduled(
            fixedDelayString = "${paypulse.processing.scheduler.stuck-batch-recovery-fixed-delay-millis:30000}",
            initialDelayString = "${paypulse.processing.scheduler.stuck-batch-recovery-initial-delay-millis:20000}"
    )
    public void recoverStuckProcessingBatches() {
        try {
            int recoveredCount = processingBatchRecoveryService.recoverStuckProcessingBatches();
            if (recoveredCount > 0) {
                log.warn("Recovered {} timed-out PROCESSING batches", recoveredCount);
            }
        } catch (Exception exception) {
            log.error("PROCESSING batch recovery scheduler failed", exception);
        }
    }
}

