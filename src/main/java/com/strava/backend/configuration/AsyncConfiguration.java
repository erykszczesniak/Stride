package com.strava.backend.configuration;

import io.micrometer.context.ContextExecutorService;
import io.micrometer.context.ContextSnapshotFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executor;

/**
 * Wraps the default Spring {@code @Async} executor with a Micrometer
 * {@link ContextExecutorService} so that trace/span context (and any other
 * {@link io.micrometer.context.ThreadLocalAccessor}-registered thread-locals,
 * e.g. MDC {@code traceId}) crosses the thread boundary.
 *
 * Without this, an {@code @Async} {@link org.springframework.context.event.EventListener}
 * (like {@code NotificationEventListener}) would start a brand-new root span
 * instead of becoming a child of the caller's span.
 */
@Configuration
public class AsyncConfiguration implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(4);
        taskExecutor.setMaxPoolSize(16);
        taskExecutor.setQueueCapacity(100);
        taskExecutor.setThreadNamePrefix("stride-async-");
        taskExecutor.initialize();

        ExecutorService delegate = taskExecutor.getThreadPoolExecutor();
        return ContextExecutorService.wrap(delegate, ContextSnapshotFactory.builder().build()::captureAll);
    }
}
