package com.financeiro.paymentmethod.interfaces.rest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.financeiro.company.application.CompanyNotFoundException;
import com.financeiro.company.application.PageResult;
import com.financeiro.company.interfaces.rest.PageQueryParser;
import com.financeiro.interfaces.rest.error.*;
import com.financeiro.interfaces.rest.trace.TraceIdFilter;
import com.financeiro.paymentmethod.application.*;
import com.financeiro.paymentmethod.domain.PaymentMethod;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentMethodController.class)
@ActiveProfiles("test")
@Import({
  PageQueryParser.class,
  GlobalExceptionHandler.class,
  TraceIdProvider.class,
  TraceIdFilter.class
})
class PaymentMethodControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean CreatePaymentMethod create;
  @MockitoBean GetPaymentMethod get;
  @MockitoBean ListPaymentMethodsByCompany list;
  @MockitoBean DeactivatePaymentMethod deactivate;

  @Test
  void allEndpointsFollowContract() throws Exception {
    var value = PaymentMethod.rehydrate(7L, 3L, "PIX", true);
    when(create.execute(3L, "PIX")).thenReturn(value);
    when(get.execute(3L, 7L)).thenReturn(value);
    when(list.execute(eq(3L), any())).thenReturn(new PageResult<>(List.of(value), 0, 1, 1, 1));
    when(deactivate.execute(3L, 7L)).thenReturn(PaymentMethod.rehydrate(7L, 3L, "PIX", false));

    mvc.perform(
            post("/api/v1/companies/3/payment-methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"PIX\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/companies/3/payment-methods/7"));
    mvc.perform(get("/api/v1/companies/3/payment-methods/7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.companyId").value(3));
    mvc.perform(get("/api/v1/companies/3/payment-methods?page=0&size=1&sort=id,desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meta.totalPages").value(1));
    mvc.perform(post("/api/v1/companies/3/payment-methods/7/deactivate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
  }

  @Test
  void rejectsUnknownFieldsAndInvalidPagination() throws Exception {
    for (String field : List.of("id", "companyId", "active", "unknown")) {
      mvc.perform(
              post("/api/v1/companies/3/payment-methods")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"name\":\"X\",\"" + field + "\":1}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }
    for (String sort : List.of("id,asc", "id,desc", "name,asc", "name,desc")) {
      when(list.execute(eq(3L), any())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
      mvc.perform(get("/api/v1/companies/3/payment-methods?sort=" + sort))
          .andExpect(status().isOk());
    }
    mvc.perform(get("/api/v1/companies/3/payment-methods?size=101"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    for (String query : List.of("sort=id", "sort=active,asc", "sort=id,up")) {
      mvc.perform(get("/api/v1/companies/3/payment-methods?" + query))
          .andExpect(status().isUnprocessableContent())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
  }

  @Test
  void mapsMissingCompanyDuringCreate() throws Exception {
    when(create.execute(9L, "PIX")).thenThrow(new CompanyNotFoundException(9L));
    mvc.perform(
            post("/api/v1/companies/9/payment-methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"PIX\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
  }

  @Test
  void crossCompanyMissingDoesNotLeak() throws Exception {
    when(get.execute(2L, 7L)).thenThrow(new PaymentMethodNotFoundException(2L, 7L));
    mvc.perform(get("/api/v1/companies/2/payment-methods/7"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PAYMENT_METHOD_NOT_FOUND"))
        .andExpect(jsonPath("$.traceId").exists());
  }
}
