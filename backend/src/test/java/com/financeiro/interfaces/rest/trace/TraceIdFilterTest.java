package com.financeiro.interfaces.rest.trace;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void removeTraceId() {
        MDC.remove(TraceContext.MDC_KEY);
    }

    @Test
    void makesGeneratedTraceIdAvailableDuringRequestAndReturnsItInResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> downstreamTraceId = new AtomicReference<>();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                downstreamTraceId.set(MDC.get(TraceContext.MDC_KEY)));

        String responseTraceId = response.getHeader(TraceContext.HEADER_NAME);
        assertThat(downstreamTraceId.get()).isEqualTo(responseTraceId);
        assertThat(UUID.fromString(responseTraceId).toString()).isEqualTo(responseTraceId);
    }

    @Test
    void removesOwnedMdcValueAfterSuccessfulRequest() throws Exception {
        filter.doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                (ignoredRequest, ignoredResponse) -> assertThat(MDC.get(TraceContext.MDC_KEY)).isNotNull());

        assertThat(MDC.get(TraceContext.MDC_KEY)).isNull();
    }

    @Test
    void removesOwnedMdcValueAfterExceptionalRequest() {
        assertThatThrownBy(() -> filter.doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                (ignoredRequest, ignoredResponse) -> {
                    throw new ServletException("test failure");
                }))
                .isInstanceOf(ServletException.class)
                .hasMessage("test failure");

        assertThat(MDC.get(TraceContext.MDC_KEY)).isNull();
    }

    @Test
    void generatesDifferentTraceIdsForSeparateRequests() throws Exception {
        MockHttpServletResponse firstResponse = performRequest();
        MockHttpServletResponse secondResponse = performRequest();

        assertThat(firstResponse.getHeader(TraceContext.HEADER_NAME))
                .isNotEqualTo(secondResponse.getHeader(TraceContext.HEADER_NAME));
    }

    @Test
    void ignoresClientProvidedTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceContext.HEADER_NAME, "client-controlled-trace-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                assertThat(MDC.get(TraceContext.MDC_KEY)).isNotEqualTo("client-controlled-trace-id"));

        assertThat(response.getHeader(TraceContext.HEADER_NAME)).isNotEqualTo("client-controlled-trace-id");
    }

    private MockHttpServletResponse performRequest() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), response, (ignoredRequest, ignoredResponse) -> {
        });
        return response;
    }
}
