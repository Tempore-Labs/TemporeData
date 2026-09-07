package org.temporedata.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async executor wiring (data-access governance audit reads the tail of the
 * tamper-evident hash chain, so governance audit submissions must be serialized).
 *
 * <p>{@code governanceAuditExecutor} is a bounded single-pool executor scoped to the
 * asynchronous audit fan-out (§4.6 异步审计、不阻塞主链路) so it never blocks the query
 * hot path and keeps {@code zy_audit_event.seq/prev_hash} ordering deterministic.</p>
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "governanceAuditExecutor")
    public Executor governanceAuditExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(1);
        ex.setMaxPoolSize(1);
        ex.setQueueCapacity(2048);
        ex.setThreadNamePrefix("td-governaudit-");
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.initialize();
        return ex;
    }
}