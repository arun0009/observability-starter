package com.company.observability.kafka;

import com.company.observability.core.MdcKeys;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Kafka producer interceptor that injects observability context from MDC
 * into outgoing Kafka message headers, enabling trace correlation across
 * asynchronous boundaries.
 */
public class ObservabilityKafkaProducerInterceptor<K, V> implements ProducerInterceptor<K, V> {

    @Override
    public ProducerRecord<K, V> onSend(ProducerRecord<K, V> record) {
        injectIfPresent(record, MdcKeys.REQUEST_ID, MdcKeys.HEADER_REQUEST_ID);
        injectIfPresent(record, MdcKeys.CORRELATION_ID, MdcKeys.HEADER_CORRELATION_ID);
        injectIfPresent(record, MdcKeys.USER_ID, MdcKeys.HEADER_USER_ID);
        injectIfPresent(record, MdcKeys.TENANT_ID, MdcKeys.HEADER_TENANT_ID);
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // no-op
    }

    private void injectIfPresent(ProducerRecord<K, V> record, String mdcKey, String headerName) {
        String value = MDC.get(mdcKey);
        if (value != null) {
            record.headers().add(headerName, value.getBytes(StandardCharsets.UTF_8));
        }
    }
}
