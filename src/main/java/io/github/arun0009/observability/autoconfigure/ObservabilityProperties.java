package io.github.arun0009.observability.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Centralized configuration properties for the observability starter.
 * <p>
 * Example {@code application.yml}:
 * 
 * <pre>
 * observability:
 *   mdc:
 *     enabled: true
 *   trace-guard:
 *     enabled: true
 *     fail-on-missing: false
 *   sampling:
 *     probability: 1.0
 *   async:
 *     propagation-enabled: true
 *   kafka:
 *     propagation-enabled: true
 *   exception-handler:
 *     enabled: true
 *   audit:
 *     enabled: true
 * </pre>
 */
@ConfigurationProperties(prefix = "observability")
public class ObservabilityProperties {

    private final Mdc mdc = new Mdc();
    private final TraceGuard traceGuard = new TraceGuard();
    private final Sampling sampling = new Sampling();
    private final Async async = new Async();
    private final Kafka kafka = new Kafka();
    private final ExceptionHandler exceptionHandler = new ExceptionHandler();
    private final Audit audit = new Audit();

    public Mdc getMdc() {
        return mdc;
    }

    public TraceGuard getTraceGuard() {
        return traceGuard;
    }

    public Sampling getSampling() {
        return sampling;
    }

    public Async getAsync() {
        return async;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public ExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    public Audit getAudit() {
        return audit;
    }

    public static class Mdc {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class TraceGuard {
        private boolean enabled = true;
        private boolean failOnMissing = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isFailOnMissing() {
            return failOnMissing;
        }

        public void setFailOnMissing(boolean failOnMissing) {
            this.failOnMissing = failOnMissing;
        }
    }

    public static class Sampling {
        private double probability = 1.0;

        public double getProbability() {
            return probability;
        }

        public void setProbability(double probability) {
            this.probability = probability;
        }
    }

    public static class Async {
        private boolean propagationEnabled = true;

        public boolean isPropagationEnabled() {
            return propagationEnabled;
        }

        public void setPropagationEnabled(boolean propagationEnabled) {
            this.propagationEnabled = propagationEnabled;
        }
    }

    public static class Kafka {
        private boolean propagationEnabled = true;

        public boolean isPropagationEnabled() {
            return propagationEnabled;
        }

        public void setPropagationEnabled(boolean propagationEnabled) {
            this.propagationEnabled = propagationEnabled;
        }
    }

    public static class ExceptionHandler {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Audit {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
