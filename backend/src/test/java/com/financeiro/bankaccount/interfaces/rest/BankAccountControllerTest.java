package com.financeiro.bankaccount.interfaces.rest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.financeiro.bankaccount.application.*;
import com.financeiro.bankaccount.domain.BankAccount;
import com.financeiro.company.application.BranchNotFoundException;
import com.financeiro.company.application.CompanyNotFoundException;
import com.financeiro.company.application.PageResult;
import com.financeiro.company.interfaces.rest.PageQueryParser;
import com.financeiro.interfaces.rest.error.*;
import com.financeiro.interfaces.rest.trace.TraceIdFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BankAccountController.class)
@ActiveProfiles("test")
@Import({
  PageQueryParser.class,
  GlobalExceptionHandler.class,
  TraceIdProvider.class,
  TraceIdFilter.class
})
class BankAccountControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean CreateBankAccount create;
  @MockitoBean GetBankAccount get;
  @MockitoBean ListBankAccountsByCompany list;
  @MockitoBean DeactivateBankAccount deactivate;

  @Test
  void allEndpointsFollowContract() throws Exception {
    var value = BankAccount.rehydrate(7L, 3L, 5L, "Operating", true);
    when(create.execute(3L, 5L, "Operating")).thenReturn(value);
    when(get.execute(3L, 7L)).thenReturn(value);
    when(list.execute(eq(3L), any())).thenReturn(new PageResult<>(List.of(value), 0, 1, 1, 1));
    when(deactivate.execute(3L, 7L))
        .thenReturn(BankAccount.rehydrate(7L, 3L, 5L, "Operating", false));

    mvc.perform(
            post("/api/v1/companies/3/bank-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Operating\",\"branchId\":5}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/companies/3/bank-accounts/7"))
        .andExpect(jsonPath("$.companyId").value(3))
        .andExpect(jsonPath("$.branchId").value(5));
    mvc.perform(get("/api/v1/companies/3/bank-accounts/7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));
    mvc.perform(get("/api/v1/companies/3/bank-accounts?page=0&size=1&sort=name,desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meta.totalElements").value(1));
    mvc.perform(post("/api/v1/companies/3/bank-accounts/7/deactivate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
  }

  @Test
  void acceptsOmittedBranchAndRejectsUnknownOwnershipOrStatus() throws Exception {
    when(create.execute(3L, null, "General"))
        .thenReturn(BankAccount.rehydrate(8L, 3L, null, "General", true));
    mvc.perform(
            post("/api/v1/companies/3/bank-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"General\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.branchId").doesNotExist());

    mvc.perform(
            post("/api/v1/companies/3/bank-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"General\",\"branchId\":null}"))
        .andExpect(status().isCreated());

    for (String field : List.of("id", "companyId", "active", "unknown")) {
      mvc.perform(
              post("/api/v1/companies/3/bank-accounts")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"name\":\"X\",\"" + field + "\":1}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }
  }

  @Test
  void validatesBranchIdAndMapsCreateOwnershipErrors() throws Exception {
    for (int branchId : List.of(0, -1)) {
      mvc.perform(
              post("/api/v1/companies/3/bank-accounts")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"name\":\"X\",\"branchId\":" + branchId + "}"))
          .andExpect(status().isUnprocessableContent())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    when(create.execute(3L, 5L, "Restricted")).thenThrow(new BranchNotFoundException(5L));
    mvc.perform(
            post("/api/v1/companies/3/bank-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Restricted\",\"branchId\":5}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("BRANCH_NOT_FOUND"));

    when(create.execute(9L, null, "General")).thenThrow(new CompanyNotFoundException(9L));
    mvc.perform(
            post("/api/v1/companies/9/bank-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"General\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
  }

  @Test
  void validatesPaginationSortAndStableNotFound() throws Exception {
    for (String sort : List.of("id,asc", "id,desc", "name,asc", "name,desc")) {
      when(list.execute(eq(3L), any())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
      mvc.perform(get("/api/v1/companies/3/bank-accounts?sort=" + sort)).andExpect(status().isOk());
    }
    for (String query :
        List.of("page=-1", "size=0", "size=101", "sort=active,asc", "sort=id,up", "sort=id")) {
      mvc.perform(get("/api/v1/companies/3/bank-accounts?" + query))
          .andExpect(status().isUnprocessableContent())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
    when(get.execute(2L, 7L)).thenThrow(new BankAccountNotFoundException(2L, 7L));
    mvc.perform(get("/api/v1/companies/2/bank-accounts/7"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("BANK_ACCOUNT_NOT_FOUND"))
        .andExpect(jsonPath("$.traceId").exists());
  }
}
