# Observability Starter

A Spring Boot starter that gives your microservices enterprise-grade observability **with zero code changes**. Add the dependency and you get structured JSON logging, distributed tracing, metrics, and guardrails out of the box.

## Quick Start

```xml
<dependency>
    <groupId>com.company</groupId>
    <artifactId>observability-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

That's it. Your service now has:
- JSON structured logging with traceId, requestId, userId
- Distributed trace propagation (RestTemplate, WebClient, Kafka)
- MDC context in `@Async` threads
- `@Scheduled` task instrumentation
- Global exception enrichment
- SLO metrics (error ratio, p99 latency)
- Missing trace detection guardrail

---

## Features

### 1. Structured JSON Logging

Every log line is JSON with automatic MDC enrichment:

```json
{
  "message": "Order created",
  "level": "INFO",
  "mdc": { "traceId": "abc123...", "spanId": "def456..." },
  "context": {
    "service": "order-service",
    "env": "production",
    "requestId": "req-789",
    "userId": "user-42",
    "tenantId": "acme-corp",
    "correlationId": "corr-001"
  }
}
```

MDC keys are populated automatically from inbound HTTP headers:

| HTTP Header | MDC Key |
|---|---|
| `X-Request-ID` | `requestId` (auto-generated if missing) |
| `X-User-ID` | `userId` |
| `X-Tenant-ID` | `tenantId` |
| `X-Correlation-ID` | `correlationId` |

#### Custom MDC Keys

Implement `MdcContributor` to add your own MDC keys:

```java
@Component
public class RegionMdcContributor implements MdcContributor {
    @Override
    public void contribute(HttpServletRequest request) {
        String region = request.getHeader("X-Region");
        if (region != null) {
            MDC.put("region", region);
        }
    }
}
```

---

### 2. Distributed Trace Propagation

Trace context and MDC headers are automatically propagated on outgoing calls.

**RestTemplate** — works automatically for all `RestTemplate` beans:

```java
@Autowired RestTemplate restTemplate;

// X-Request-ID, X-User-ID, X-Tenant-ID, X-Correlation-ID
// are automatically added as headers, plus OTel trace headers
restTemplate.getForObject("http://downstream/api", String.class);
```

**WebClient** — works automatically for all `WebClient.Builder` beans:

```java
@Autowired WebClient.Builder webClientBuilder;

webClientBuilder.build()
    .get().uri("http://downstream/api")
    .retrieve().bodyToMono(String.class);
// Same headers propagated automatically
```

**Kafka** — producer/consumer interceptors are auto-registered when `spring-kafka` is on the classpath:

```java
// Producer: MDC context is injected into Kafka record headers automatically
kafkaTemplate.send("orders", order);

// Consumer: MDC context is extracted from Kafka record headers automatically
@KafkaListener(topics = "orders")
public void handle(Order order) {
    log.info("Processing order"); // Has traceId, requestId, etc.
}
```

---

### 3. Async Context Propagation

MDC is **thread-local**, which means it's lost when work dispatches to another thread. This starter fixes that automatically.

**`@Async` methods** — just work, no changes needed:

```java
@Async
public void sendWelcomeEmail(String userId) {
    // traceId, requestId, userId are all present in MDC
    log.info("Sending welcome email to {}", userId);
}
```

**Custom thread pools** — apply the decorator manually:

```java
@Bean
public ThreadPoolTaskExecutor myExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setTaskDecorator(new ObservabilityTaskDecorator());
    executor.initialize();
    return executor;
}
```

**`CompletableFuture`** — wrap with the decorator:

```java
ObservabilityTaskDecorator decorator = new ObservabilityTaskDecorator();
CompletableFuture.runAsync(decorator.decorate(() -> {
    log.info("Has full MDC context");
}), executor);
```

---

### 4. Scheduled Task Instrumentation

`@Scheduled` methods are automatically instrumented with:
- MDC population (service, env, synthetic requestId)
- Duration metric (`scheduled.task.duration`)
- Error counter (`scheduled.task.errors`)

```java
@Scheduled(fixedRate = 60000)
public void cleanupExpiredSessions() {
    // Logs include service context
    // Duration is tracked as a metric
    // Errors are counted
    log.info("Cleaning up sessions");
}
```

---

### 5. Global Exception Handling

Unhandled exceptions are automatically:
- Logged with full MDC context (traceId, requestId, userId)
- Recorded on the active OTel span (marked as ERROR)
- Returned as RFC 7807 Problem Detail with a `requestId` for support correlation

```json
{
  "type": "urn:error:internal",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "An internal error occurred. Reference: req-789",
  "requestId": "req-789",
  "traceId": "abc123..."
}
```

---

### 6. Business Metrics

Simple facade for custom metrics — no need to wire `MeterRegistry` directly:

```java
@Autowired BusinessMetrics metrics;

// Count events
metrics.count("orders.placed", "region", "us-east");

// Time operations
Order order = metrics.timed("orders.processing",
    new String[]{"type", "express"},
    () -> orderService.process(request));

// Register gauges
metrics.gauge("queue.depth", () -> queue.size(), "queue", "orders");
```

---

### 7. Audit Logging

Structured audit events routed to a dedicated `AUDIT` logger (can be sent to a separate sink):

```java
@Autowired AuditLogger auditLogger;

auditLogger.log("USER_LOGIN", "user@example.com", "/api/login", "SUCCESS");
auditLogger.log("DATA_EXPORT", "admin", "/api/export", "SUCCESS", "Exported 1000 records");
```

Output:
```json
{
  "message": "AUDIT event=USER_LOGIN actor=user@example.com resource=/api/login outcome=SUCCESS",
  "logger": "AUDIT",
  "context": {
    "audit.action": "USER_LOGIN",
    "audit.actor": "user@example.com",
    "audit.outcome": "SUCCESS",
    "audit.resource": "/api/login",
    "traceId": "abc123..."
  }
}
```

---

### 8. SLO Metrics

Automatically computed and exported:

| Metric | Description |
|---|---|
| `slo.http.error.ratio` | Ratio of 5xx responses to total requests |
| `slo.http.latency.p99.ms` | 99th percentile HTTP latency in milliseconds |

Use these in Grafana/Datadog alerts to track SLO compliance.

---

### 9. Trace Guard

Detects incoming requests missing trace propagation headers (`traceparent`, `X-B3-TraceId`) and:
- Logs a warning
- Increments `trace.missing.total` counter
- Optionally rejects the request (for strict environments)

---

## Configuration

All features are **enabled by default** and individually toggleable:

```yaml
spring:
  application:
    name: my-service

app:
  env: production

observability:
  mdc:
    enabled: true                        # MDC filter
  trace-guard:
    enabled: true                        # Missing trace detection
    fail-on-missing: false               # Set true to reject requests without trace
  sampling:
    probability: 1.0                     # Trace sampling rate (0.0 to 1.0)
  async:
    propagation-enabled: true            # MDC in @Async threads
  kafka:
    propagation-enabled: true            # Kafka header propagation
  exception-handler:
    enabled: true                        # Global exception enrichment
  audit:
    enabled: true                        # Audit logger
```

---

## Architecture

```
observability-starter
├── autoconfigure/          # Auto-configuration + properties
├── core/                   # MdcFilter, MdcKeys, TraceGuardFilter, MdcContributor
├── async/                  # TaskDecorator + executor config
├── kafka/                  # Producer/consumer interceptors
├── propagation/            # RestTemplate + WebClient header propagation
├── metrics/                # StandardMetrics, BusinessMetrics, SLO
├── guardrails/             # Sampler enforcement + resource attributes
├── scheduling/             # @Scheduled AOP aspect
├── exception/              # Global exception handler
└── audit/                  # Structured audit logger
```

## Requirements

- Java 17+
- Spring Boot 3.x
- Log4j2 (starter excludes Logback)
