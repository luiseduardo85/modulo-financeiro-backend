package com.financeiro.interfaces.rest.error;

import com.financeiro.interfaces.rest.trace.TraceContext;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public final class TraceIdProvider {

  public String currentTraceId() {
    return MDC.get(TraceContext.MDC_KEY);
  }
}
