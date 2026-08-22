package com.financeiro.interfaces.rest.trace;

public final class TraceContext {

  public static final String MDC_KEY = "traceId";
  public static final String HEADER_NAME = "X-Trace-Id";

  private TraceContext() {}
}
