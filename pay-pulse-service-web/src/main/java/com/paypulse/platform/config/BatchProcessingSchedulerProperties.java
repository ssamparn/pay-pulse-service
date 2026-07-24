package com.paypulse.platform.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "paypulse.processing.scheduler")
public class BatchProcessingSchedulerProperties {

    private int batchSize = 50;
    private long fixedDelayMillis = 5000;
    private long initialDelayMillis = 10000;

    private int stuckBatchRecoveryBatchSize = 50;
    private long stuckBatchTimeoutMillis = 300000;
    private long stuckBatchRecoveryFixedDelayMillis = 30000;
    private long stuckBatchRecoveryInitialDelayMillis = 20000;
    private int maxRecoveryAttempts = 3;
}

