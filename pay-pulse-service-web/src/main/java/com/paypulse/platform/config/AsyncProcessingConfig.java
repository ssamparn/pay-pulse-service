package com.paypulse.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncProcessingConfig {

    @Bean(name = "batchPersistenceExecutor")
    public Executor batchPersistenceExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}

