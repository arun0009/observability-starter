package io.github.arun0009.observability.async;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

/**
 * Auto-configures a default {@link ThreadPoolTaskExecutor} with the
 * {@link ObservabilityTaskDecorator} so that MDC context is propagated
 * to {@code @Async} methods automatically.
 * <p>
 * If a service already defines its own {@code TaskExecutor}, it should
 * apply the {@link ObservabilityTaskDecorator} manually.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "observability.async", name = "propagation-enabled", havingValue = "true", matchIfMissing = true)
public class ObservabilityExecutorConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(new ObservabilityTaskDecorator());
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("obs-async-");
        executor.initialize();
        return executor;
    }
}
