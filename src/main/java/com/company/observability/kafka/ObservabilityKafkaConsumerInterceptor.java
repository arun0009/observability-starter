package com.company.observability.kafka;

import com.company.observability.core.MdcKeys;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Kafka consumer interceptor that extracts observability headers from
 * incoming Kafka messages and places them into MDC for downstream logging.
 */
public class ObservabilityKafkaConsumerInterceptor<K, V> implements ConsumerInterceptor<K, V> {

    @Override
    public ConsumerRecords<K, V> onConsume(ConsumerRecords<K, V> records) {
        // For batch processing, we apply MDC from the first record.
        // Per-record MDC should be done in the listener using the headers directly.
        records.forEach(record -> {
            extractHeader(record.headers(), MdcKeys.HEADER_REQUEST_ID, MdcKeys.REQUEST_ID);
            extractHeader(record.headers(), MdcKeys.HEADER_CORRELATION_ID, MdcKeys.CORRELATION_ID);
            extractHeader(record.headers(), MdcKeys.HEADER_USER_ID, MdcKeys.USER_ID);
            extractHeader(record.headers(), MdcKeys.HEADER_TENANT_ID, MdcKeys.TENANT_ID);
        });
        return records;
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
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

    private void extractHeader(org.apache.kafka.common.header.Headers headers, String headerName, String mdcKey) {
        Header header = headers.lastHeader(headerName);
        if (header != null) {
            MDC.put(mdcKey, new String(header.value(), StandardCharsets.UTF_8));
        }
    }
}
