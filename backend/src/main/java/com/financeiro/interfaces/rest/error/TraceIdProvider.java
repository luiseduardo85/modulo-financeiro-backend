package com.financeiro.interfaces.rest.error;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public final class TraceIdProvider {

    public static final String MDC_KEY = "traceId";

    public String currentTraceId() {
        return MDC.get(MDC_KEY);
    }
}
