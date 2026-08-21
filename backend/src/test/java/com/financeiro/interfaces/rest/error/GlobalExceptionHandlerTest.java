package com.financeiro.interfaces.rest.error;

import java.time.Instant;

import com.financeiro.interfaces.rest.trace.TraceContext;
import com.financeiro.interfaces.rest.trace.TraceIdFilter;
import com.jayway.jsonpath.JsonPath;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ExtendWith(OutputCaptureExtension.class)
@Import({
        GlobalExceptionHandler.class,
        TraceIdProvider.class,
        TraceIdFilter.class,
        GlobalExceptionHandlerTest.TestController.class
})
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    void clearMdc() {
        MDC.remove(TraceContext.MDC_KEY);
    }

    @Test
    void mapsBeanValidationToUnprocessableEntity() throws Exception {
        mockMvc.perform(post("/test/errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"quantity\":0}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("name"))
                .andExpect(jsonPath("$.details[0].code").value("NOT_BLANK"))
                .andExpect(jsonPath("$.details[1].field").value("quantity"))
                .andExpect(jsonPath("$.details[1].code").value("POSITIVE"));
    }

    @Test
    void mapsMalformedJsonToBadRequest() throws Exception {
        mockMvc.perform(post("/test/errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void mapsTechnicalNotFound() throws Exception {
        assertTechnicalError("not-found", 404, "RESOURCE_NOT_FOUND");
    }

    @Test
    void mapsTechnicalConflict() throws Exception {
        assertTechnicalError("conflict", 409, "CONFLICT");
    }

    @Test
    void mapsTechnicalSemanticValidation() throws Exception {
        assertTechnicalError("semantic-validation", 422, "VALIDATION_ERROR");
    }

    @Test
    void mapsUnexpectedExceptionToSafeInternalErrorWithRequestTraceId(CapturedOutput output) throws Exception {
        MvcResult result = mockMvc.perform(get("/test/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An internal error occurred."))
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(mvcResult -> assertThat(mvcResult.getResponse().getContentAsString())
                        .doesNotContain("sensitive internal detail"))
                .andReturn();

        String responseTraceId = result.getResponse().getHeader(TraceContext.HEADER_NAME);
        String bodyTraceId = JsonPath.read(result.getResponse().getContentAsString(), "$.traceId");
        assertThat(bodyTraceId).isEqualTo(responseTraceId);
        assertThat(output).contains("Unexpected REST error", responseTraceId);
    }

    @Test
    void returnsIso8601TimestampAndRequestTraceId() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/errors/not-found"))
                .andExpect(status().isNotFound())
                .andReturn();

        String timestamp = JsonPath.read(result.getResponse().getContentAsString(), "$.timestamp");
        String traceId = JsonPath.read(result.getResponse().getContentAsString(), "$.traceId");
        assertThat(Instant.parse(timestamp)).isNotNull();
        assertThat(traceId).isEqualTo(result.getResponse().getHeader(TraceContext.HEADER_NAME));
        assertThat(MDC.get(TraceContext.MDC_KEY)).isNull();
    }

    private void assertTechnicalError(String endpoint, int expectedStatus, String code) throws Exception {
        mockMvc.perform(get("/test/errors/{endpoint}", endpoint))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(expectedStatus))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @RestController
    @RequestMapping("/test/errors")
    static class TestController {

        @PostMapping("/validation")
        void validate(@Valid @RequestBody TestRequest request) {
            // Test-only endpoint used to exercise Spring MVC request validation.
        }

        @GetMapping("/{type}")
        void technicalError(@PathVariable String type) {
            throw switch (type) {
                case "not-found" -> new ApiErrorException(ApiErrorType.RESOURCE_NOT_FOUND, "Resource not found.");
                case "conflict" -> new ApiErrorException(ApiErrorType.CONFLICT, "A conflict occurred.");
                case "semantic-validation" -> new ApiErrorException(
                        ApiErrorType.SEMANTIC_VALIDATION, "The input is semantically invalid.");
                case "unexpected" -> new IllegalStateException("sensitive internal detail");
                default -> new IllegalArgumentException("Unsupported test error type");
            };
        }
    }

    record TestRequest(@NotBlank String name, @Positive int quantity) {
    }
}
