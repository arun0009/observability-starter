package io.github.arun0009.observability.propagation;

import io.github.arun0009.observability.core.MdcKeys;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Auto-configures every {@link WebClient.Builder} bean with a filter
 * that propagates MDC context as HTTP headers on outgoing requests.
 * <p>
 * Important: In a fully reactive stack, MDC is not reliable because
 * Reactor uses different threads. This works correctly when WebClient
 * is used in a servlet (blocking) context, which is the common
 * enterprise pattern.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(WebClient.class)
public class WebClientPropagationConfiguration {

    @Bean
    public WebClientCustomizer mdcPropagationWebClientCustomizer() {
        return webClientBuilder -> webClientBuilder.filter(mdcPropagationFilter());
    }

    private ExchangeFilterFunction mdcPropagationFilter() {
        return (request, next) -> {
            ClientRequest.Builder requestBuilder = ClientRequest.from(request);

            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            if (contextMap != null) {
                propagateIfPresent(contextMap, MdcKeys.USER_ID, MdcKeys.HEADER_USER_ID, requestBuilder);
                propagateIfPresent(contextMap, MdcKeys.REQUEST_ID, MdcKeys.HEADER_REQUEST_ID, requestBuilder);
                propagateIfPresent(contextMap, MdcKeys.TENANT_ID, MdcKeys.HEADER_TENANT_ID, requestBuilder);
                propagateIfPresent(contextMap, MdcKeys.CORRELATION_ID, MdcKeys.HEADER_CORRELATION_ID, requestBuilder);
            }

            return next.exchange(requestBuilder.build());
        };
    }

    private void propagateIfPresent(Map<String, String> contextMap, String mdcKey,
            String headerName, ClientRequest.Builder requestBuilder) {
        String value = contextMap.get(mdcKey);
        if (value != null) {
            requestBuilder.header(headerName, value);
        }
    }
}
