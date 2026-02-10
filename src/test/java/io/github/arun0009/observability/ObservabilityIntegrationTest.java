package io.github.arun0009.observability;

import io.github.arun0009.observability.audit.AuditLogger;
import io.github.arun0009.observability.metrics.BusinessMetrics;
import io.github.arun0009.observability.testapp.TestApplication;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ObservabilityIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void coreBeansAreRegistered() {
        assertThat(applicationContext.containsBean("mdcFilter")).isTrue();
        assertThat(applicationContext.containsBean("traceGuardFilter")).isTrue();
        assertThat(applicationContext.containsBean("defaultSampler")).isTrue();
        assertThat(applicationContext.containsBean("openTelemetryResource")).isTrue();
    }

    @Test
    void advancedBeansAreRegistered() {
        assertThat(applicationContext.containsBean("businessMetrics")).isTrue();
        assertThat(applicationContext.containsBean("auditLogger")).isTrue();
        assertThat(applicationContext.containsBean("observabilityExceptionHandler")).isTrue();
        assertThat(applicationContext.containsBean("scheduledTaskObservabilityAspect")).isTrue();
    }

    @Test
    void httpRequestGeneratesMetrics() {
        ResponseEntity<String> response = restTemplate.getForEntity("/hello", String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(meterRegistry.find("http.server.requests").timer()).isNotNull();
    }

    @Test
    void businessMetricsFacadeWorks() {
        BusinessMetrics metrics = applicationContext.getBean(BusinessMetrics.class);
        metrics.count("test.orders.placed", "region", "us-east");
        assertThat(meterRegistry.find("test.orders.placed").counter()).isNotNull();
    }

    @Test
    void auditLoggerIsAvailable() {
        AuditLogger auditLogger = applicationContext.getBean(AuditLogger.class);
        // Should not throw
        auditLogger.log("TEST_ACTION", "test-user", "/test", "SUCCESS");
    }

    @Test
    void sloGaugesAreRegistered() {
        // Hit endpoint to populate http.server.requests
        restTemplate.getForEntity("/hello", String.class);

        assertThat(meterRegistry.find("slo.http.error.ratio").gauge()).isNotNull();
        assertThat(meterRegistry.find("slo.http.latency.p99.ms").gauge()).isNotNull();
    }
}
