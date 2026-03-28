package com.taskflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configures the thread pool used by @Async methods.
 * Demonstrates Concurrency — thread pool management.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${taskflow.async.core-pool-size:5}")
    private int corePoolSize;

    @Value("${taskflow.async.max-pool-size:10}")
    private int maxPoolSize;

    @Value("${taskflow.async.queue-capacity:100}")
    private int queueCapacity;

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("TaskFlow-Async-");
        executor.initialize();
        return executor;
    }
}
