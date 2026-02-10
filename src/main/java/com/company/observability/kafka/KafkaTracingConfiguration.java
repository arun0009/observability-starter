package com.company.observability.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaConsumerFactoryCustomizer;

import java.util.Map;

/**
 * Auto-configures Kafka producer and consumer factory customizers that
 * register the observability interceptors. This ensures that all Kafka
 * interactions automatically propagate trace context.
 * <p>
 * Conditional on spring-kafka being on the classpath and the feature being
 * enabled.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ KafkaTemplate.class, ConcurrentKafkaListenerContainerFactory.class })
@ConditionalOnProperty(prefix = "observability.kafka", name = "propagation-enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTracingConfiguration {

    @Bean
    public DefaultKafkaProducerFactoryCustomizer observabilityProducerCustomizer() {
        return producerFactory -> {
            Map<String, Object> config = Map.of(
                    org.apache.kafka.clients.producer.ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
                    ObservabilityKafkaProducerInterceptor.class.getName());
            producerFactory.updateConfigs(config);
        };
    }

    @Bean
    public DefaultKafkaConsumerFactoryCustomizer observabilityConsumerCustomizer() {
        return consumerFactory -> {
            Map<String, Object> config = Map.of(
                    org.apache.kafka.clients.consumer.ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG,
                    ObservabilityKafkaConsumerInterceptor.class.getName());
            consumerFactory.updateConfigs(config);
        };
    }
}
