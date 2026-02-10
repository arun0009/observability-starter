package io.github.arun0009.observability.propagation;

import io.github.arun0009.observability.core.MdcKeys;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

/**
 * Auto-configures every {@link RestTemplate} bean with an interceptor
 * that propagates MDC context (requestId, userId, tenantId, correlationId)
 * as HTTP headers on outgoing requests.
 * <p>
 * Trace context (traceparent) is handled separately by
 * micrometer-tracing / OTel.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RestTemplate.class)
public class RestTemplatePropagationConfiguration {

    @Bean
    public RestTemplateCustomizer mdcPropagationRestTemplateCustomizer() {
        return restTemplate -> restTemplate.getInterceptors().add(new MdcPropagationInterceptor());
    }

    static class MdcPropagationInterceptor implements ClientHttpRequestInterceptor {
        @Override
        @NonNull
        public ClientHttpResponse intercept(@NonNull HttpRequest request,
                @NonNull byte[] body,
                @NonNull ClientHttpRequestExecution execution) throws IOException {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            if (contextMap != null) {
                propagateIfPresent(contextMap, MdcKeys.USER_ID, MdcKeys.HEADER_USER_ID, request);
                propagateIfPresent(contextMap, MdcKeys.REQUEST_ID, MdcKeys.HEADER_REQUEST_ID, request);
                propagateIfPresent(contextMap, MdcKeys.TENANT_ID, MdcKeys.HEADER_TENANT_ID, request);
                propagateIfPresent(contextMap, MdcKeys.CORRELATION_ID, MdcKeys.HEADER_CORRELATION_ID, request);
            }
            return execution.execute(request, body);
        }

        private void propagateIfPresent(Map<String, String> contextMap, String mdcKey,
                String headerName, HttpRequest request) {
            String value = contextMap.get(mdcKey);
            if (value != null) {
                request.getHeaders().add(headerName, value);
            }
        }
    }
}
