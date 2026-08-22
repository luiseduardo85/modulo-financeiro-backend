package com.financeiro.financialaccount.interfaces.rest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.financeiro.approval.application.*;
import com.financeiro.approval.domain.InvalidRejectionJustificationException;
import com.financeiro.category.application.CategoryNotFoundException;
import com.financeiro.company.application.BranchNotFoundException;
import com.financeiro.company.application.CompanyNotFoundException;
import com.financeiro.company.application.PageResult;
import com.financeiro.costcenter.application.CostCenterNotFoundException;
import com.financeiro.financialaccount.application.*;
import com.financeiro.financialaccount.domain.*;
import com.financeiro.interfaces.rest.error.*;
import com.financeiro.interfaces.rest.trace.TraceIdFilter;
import com.financeiro.partner.application.PartnerNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FinancialAccountController.class)
@ActiveProfiles("test")
@Import({
  FinancialAccountPageQueryParser.class,
  GlobalExceptionHandler.class,
  TraceIdProvider.class,
  TraceIdFilter.class
})
class FinancialAccountControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean CreateFinancialAccount create;
  @MockitoBean GetFinancialAccount get;
  @MockitoBean ListFinancialAccountsByCompany list;
  @MockitoBean SubmitFinancialAccountForApproval submitForApproval;
  @MockitoBean ApproveFinancialAccount approve;
  @MockitoBean RejectFinancialAccount reject;

  @Test
  void postGetAndListFollowDistinctContractsWithoutIdempotencyHeader() throws Exception {
    when(create.execute(any())).thenReturn(account(FinancialAccountType.PAYABLE));
    when(get.execute(1L, 100L)).thenReturn(account(FinancialAccountType.RECEIVABLE));
    when(list.execute(eq(1L), any())).thenReturn(new PageResult<>(List.of(summary()), 0, 20, 1, 1));
    mvc.perform(
            post("/api/v1/companies/1/financial-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("PAYABLE")))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/companies/1/financial-accounts/100"))
        .andExpect(jsonPath("$.id").value(100))
        .andExpect(jsonPath("$.companyId").value(1))
        .andExpect(jsonPath("$.branchId").value(2))
        .andExpect(jsonPath("$.type").value("PAYABLE"))
        .andExpect(jsonPath("$.partnerId").value(3))
        .andExpect(jsonPath("$.categoryId").value(4))
        .andExpect(jsonPath("$.costCenterId").doesNotExist())
        .andExpect(jsonPath("$.issueDate").value("2026-08-21"))
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.installments[0].id").value(1001))
        .andExpect(jsonPath("$.installments[0].installmentNumber").value(1))
        .andExpect(jsonPath("$.installments[0].dueDate").value("2026-09-10"))
        .andExpect(jsonPath("$.installments[0].amount").value(100.0))
        .andExpect(jsonPath("$.installments[1].id").value(1002))
        .andExpect(jsonPath("$.installments[1].installmentNumber").value(2))
        .andExpect(jsonPath("$.installments[1].dueDate").value("2026-10-10"))
        .andExpect(jsonPath("$.installments[1].amount").value(50.0))
        .andExpect(jsonPath("$.totalAmount").value(150.0));
    mvc.perform(get("/api/v1/companies/1/financial-accounts/100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("RECEIVABLE"))
        .andExpect(jsonPath("$.id").value(100))
        .andExpect(jsonPath("$.companyId").value(1))
        .andExpect(jsonPath("$.branchId").value(2))
        .andExpect(jsonPath("$.partnerId").value(3))
        .andExpect(jsonPath("$.categoryId").value(4))
        .andExpect(jsonPath("$.issueDate").value("2026-08-21"))
        .andExpect(jsonPath("$.totalAmount").value(150.0))
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.installments.length()").value(2));
    mvc.perform(get("/api/v1/companies/1/financial-accounts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(100))
        .andExpect(jsonPath("$.data[0].companyId").value(1))
        .andExpect(jsonPath("$.data[0].branchId").value(2))
        .andExpect(jsonPath("$.data[0].type").value("PAYABLE"))
        .andExpect(jsonPath("$.data[0].partnerId").value(3))
        .andExpect(jsonPath("$.data[0].categoryId").value(4))
        .andExpect(jsonPath("$.data[0].issueDate").value("2026-08-21"))
        .andExpect(jsonPath("$.data[0].totalAmount").value(150.0))
        .andExpect(jsonPath("$.data[0].status").value("DRAFT"))
        .andExpect(jsonPath("$.data[0].installments").doesNotExist());
  }

  @Test
  void validatesEveryPositiveRequestAndPathIdentifier() throws Exception {
    for (long companyId : List.of(0L, -1L)) {
      expectValidation(
          post("/api/v1/companies/" + companyId + "/financial-accounts")
              .contentType(MediaType.APPLICATION_JSON)
              .content(body("PAYABLE")));
      expectValidation(get("/api/v1/companies/" + companyId + "/financial-accounts"));
    }
    for (long accountId : List.of(0L, -1L))
      expectValidation(get("/api/v1/companies/1/financial-accounts/" + accountId));

    for (String invalidBody :
        List.of(
            body("PAYABLE").replace("\"branchId\":2", "\"branchId\":0"),
            body("PAYABLE").replace("\"branchId\":2", "\"branchId\":-1"),
            body("PAYABLE").replace("\"partnerId\":3", "\"partnerId\":0"),
            body("PAYABLE").replace("\"partnerId\":3", "\"partnerId\":-1"),
            body("PAYABLE").replace("\"categoryId\":4", "\"categoryId\":0"),
            body("PAYABLE").replace("\"categoryId\":4", "\"categoryId\":-1"),
            body("PAYABLE").replace("\"costCenterId\":null", "\"costCenterId\":0"),
            body("PAYABLE").replace("\"costCenterId\":null", "\"costCenterId\":-1"),
            body("PAYABLE").replaceFirst("\"installmentNumber\":1", "\"installmentNumber\":0"),
            body("PAYABLE").replaceFirst("\"installmentNumber\":1", "\"installmentNumber\":-1"))) {
      expectValidation(
          post("/api/v1/companies/1/financial-accounts")
              .contentType(MediaType.APPLICATION_JSON)
              .content(invalidBody));
    }
  }

  @Test
  void mapsMalformedDatesAndEnumToMalformedRequest() throws Exception {
    for (String malformed :
        List.of(
            body("PAYABLE").replace("2026-08-21", "2026-99-99"),
            body("PAYABLE").replace("2026-09-10", "not-a-date"),
            body("INVALID_TYPE"))) {
      mvc.perform(
              post("/api/v1/companies/1/financial-accounts")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(malformed))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
          .andExpect(jsonPath("$.traceId").exists());
    }
  }

  @Test
  void locallyRejectsUnknownAndProhibitedFields() throws Exception {
    for (String field :
        List.of("id", "companyId", "status", "bankAccountId", "paymentMethodId", "unknown")) {
      mvc.perform(
              post("/api/v1/companies/1/financial-accounts")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body("PAYABLE").replaceFirst("\\{", "{\"" + field + "\":1,")))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }
    mvc.perform(
            post("/api/v1/companies/1/financial-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body("PAYABLE")
                        .replace("\"installmentNumber\":1", "\"id\":1,\"installmentNumber\":1")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
  }

  @Test
  void validatesPaginationSortAndReturnsStableErrorsWithTraceId() throws Exception {
    when(list.execute(eq(1L), any())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
    for (String sort : List.of("id,asc", "id,desc"))
      mvc.perform(get("/api/v1/companies/1/financial-accounts?sort=" + sort))
          .andExpect(status().isOk());
    for (String query :
        List.of("page=-1", "size=0", "size=101", "sort=issueDate,asc", "sort=id,up", "sort=id"))
      mvc.perform(get("/api/v1/companies/1/financial-accounts?" + query))
          .andExpect(status().isUnprocessableContent())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    when(get.execute(1L, 999L)).thenThrow(new FinancialAccountNotFoundException(999L));
    mvc.perform(get("/api/v1/companies/1/financial-accounts/999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("FINANCIAL_ACCOUNT_NOT_FOUND"))
        .andExpect(jsonPath("$.traceId").exists());
  }

  @Test
  void mapsReferenceStateErrorsAndStructuralValidation() throws Exception {
    when(create.execute(any())).thenThrow(new PartnerInactiveException());
    mvc.perform(
            post("/api/v1/companies/1/financial-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("PAYABLE")))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("PARTNER_INACTIVE"));
    assertCreateError(new PartnerRoleNotAllowedException(), "PARTNER_ROLE_NOT_ALLOWED");
    assertCreateError(new CategoryInactiveException(), "CATEGORY_INACTIVE");
    assertCreateError(new CostCenterInactiveException(), "COST_CENTER_INACTIVE");
    reset(create);
    when(create.execute(any())).thenThrow(new InvalidFinancialAccountException("invalid"));
    mvc.perform(
            post("/api/v1/companies/1/financial-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("PAYABLE").replace("150.00", "0")))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void mapsEveryMissingReferenceToStableNotFoundContract() throws Exception {
    assertCreateNotFound(new CompanyNotFoundException(1L), "COMPANY_NOT_FOUND");
    assertCreateNotFound(new BranchNotFoundException(2L), "BRANCH_NOT_FOUND");
    assertCreateNotFound(new PartnerNotFoundException(3L), "PARTNER_NOT_FOUND");
    assertCreateNotFound(new CategoryNotFoundException(1L, 4L), "CATEGORY_NOT_FOUND");
    assertCreateNotFound(new CostCenterNotFoundException(1L, 5L), "COST_CENTER_NOT_FOUND");
  }

  @Test
  void mapsDuplicateInstallmentAndTotalMismatchDomainFailures() throws Exception {
    assertStructuralError("installmentNumber must be unique");
    assertStructuralError("installment total must equal totalAmount");
  }

  @Test
  void usesExactPaginationDefaultsAndMetadata() throws Exception {
    when(list.execute(eq(1L), any())).thenReturn(new PageResult<>(List.of(summary()), 0, 20, 1, 1));
    var query = ArgumentCaptor.forClass(FinancialAccountPageQuery.class);

    mvc.perform(get("/api/v1/companies/1/financial-accounts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meta.page").value(0))
        .andExpect(jsonPath("$.meta.size").value(20))
        .andExpect(jsonPath("$.meta.totalElements").value(1))
        .andExpect(jsonPath("$.meta.totalPages").value(1));

    verify(list).execute(eq(1L), query.capture());
    org.assertj.core.api.Assertions.assertThat(query.getValue())
        .isEqualTo(new FinancialAccountPageQuery(0, 20, FinancialAccountPageQuery.Direction.ASC));
  }

  @Test
  void approvalActionRoutesReturnCompleteUpdatedAccountWithoutIdempotencyKey() throws Exception {
    when(submitForApproval.execute(1L, 100L))
        .thenReturn(account(FinancialAccountType.PAYABLE, FinancialAccountStatus.PENDING_APPROVAL));
    when(approve.execute(1L, 100L))
        .thenReturn(account(FinancialAccountType.PAYABLE, FinancialAccountStatus.APPROVED));
    when(reject.execute(1L, 100L, "fix data"))
        .thenReturn(account(FinancialAccountType.PAYABLE, FinancialAccountStatus.DRAFT));

    mvc.perform(post("/api/v1/companies/1/financial-accounts/100/submit-for-approval"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
        .andExpect(jsonPath("$.installments[0].id").value(1001));
    mvc.perform(post("/api/v1/companies/1/financial-accounts/100/approve"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"))
        .andExpect(jsonPath("$.version").doesNotExist());
    mvc.perform(
            post("/api/v1/companies/1/financial-accounts/100/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"justification\":\"fix data\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DRAFT"));
  }

  @Test
  void submitReturnsCompleteDirectApprovalForDisabledAndAbsentConfiguration() throws Exception {
    for (String scenario : List.of("disabled", "absent")) {
      reset(submitForApproval);
      when(submitForApproval.execute(1L, 100L))
          .thenReturn(account(FinancialAccountType.PAYABLE, FinancialAccountStatus.APPROVED));
      mvc.perform(
              post("/api/v1/companies/1/financial-accounts/100/submit-for-approval")
                  .header("X-Test-Scenario", scenario))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("APPROVED"))
          .andExpect(jsonPath("$.id").value(100))
          .andExpect(jsonPath("$.installments.length()").value(2))
          .andExpect(jsonPath("$.installments[0].id").value(1001));
    }
  }

  @Test
  void approvalRoutesValidatePositivePathsAndRejectUntrustedBodyFields() throws Exception {
    for (String action : List.of("submit-for-approval", "approve", "reject")) {
      for (String path :
          List.of(
              "/api/v1/companies/0/financial-accounts/100/" + action,
              "/api/v1/companies/-1/financial-accounts/100/" + action,
              "/api/v1/companies/1/financial-accounts/0/" + action,
              "/api/v1/companies/1/financial-accounts/-1/" + action)) {
        var request = post(path);
        if (action.equals("reject")) {
          request.contentType(MediaType.APPLICATION_JSON).content("{\"justification\":\"reason\"}");
        }
        expectValidation(request);
      }
    }

    for (String action : List.of("submit-for-approval", "approve")) {
      for (String field :
          List.of(
              "actorId",
              "requesterId",
              "status",
              "approvalRequired",
              "configurationId",
              "unknown")) {
        mvc.perform(
                post("/api/v1/companies/1/financial-accounts/100/" + action)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"" + field + "\":\"value\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
      }
    }
    mvc.perform(
            post("/api/v1/companies/1/financial-accounts/100/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"justification\":\"reason\",\"actorId\":\"untrusted\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    mvc.perform(
            post("/api/v1/companies/1/financial-accounts/100/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"justification\":\"reason\",\"configurationId\":123}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
  }

  @Test
  void rejectionContractMapsCandidateAndMalformedFailures() throws Exception {
    when(reject.execute(eq(1L), eq(100L), nullable(String.class)))
        .thenThrow(new InvalidRejectionJustificationException());
    for (String body :
        List.of(
            "{}",
            "{\"justification\":null}",
            "{\"justification\":\" \"}",
            "{\"justification\":\"" + "a".repeat(501) + "\"}")) {
      mvc.perform(
              post("/api/v1/companies/1/financial-accounts/100/reject")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isUnprocessableContent())
          .andExpect(jsonPath("$.code").value("REJECTION_JUSTIFICATION_REQUIRED"))
          .andExpect(jsonPath("$.traceId").exists());
    }
    mvc.perform(
            post("/api/v1/companies/1/financial-accounts/100/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
  }

  @Test
  void approvalActionsMapStableWorkflowErrorsWithTraceId() throws Exception {
    assertApprovalError(
        submitForApproval,
        new FinancialAccountNotFoundException(100L),
        "submit-for-approval",
        404,
        "FINANCIAL_ACCOUNT_NOT_FOUND");
    assertApprovalError(
        submitForApproval,
        new InvalidFinancialAccountStatusException(
            FinancialAccountStatus.APPROVED, FinancialAccountStatus.DRAFT),
        "submit-for-approval",
        409,
        "FINANCIAL_ACCOUNT_INVALID_STATUS");
    assertApprovalError(
        approve,
        new SelfApprovalNotAllowedException(),
        "approve",
        422,
        "SELF_APPROVAL_NOT_ALLOWED");
    assertApprovalError(
        approve, new ApproverNotAllowedException(), "approve", 403, "APPROVER_NOT_ALLOWED");
    assertApprovalError(
        approve, new ApprovalConflictException(), "approve", 409, "APPROVAL_CONFLICT");
  }

  @Test
  void everyApprovalActionMapsUnavailableTrustedActorToStableUnauthorizedContract()
      throws Exception {
    when(submitForApproval.execute(1L, 100L)).thenThrow(new ApprovalActorUnavailableException());
    when(approve.execute(1L, 100L)).thenThrow(new ApprovalActorUnavailableException());
    when(reject.execute(1L, 100L, "reason")).thenThrow(new ApprovalActorUnavailableException());

    for (String action : List.of("submit-for-approval", "approve")) {
      mvc.perform(
              post("/api/v1/companies/1/financial-accounts/100/" + action)
                  .header("X-Actor-Id", "cannot-bypass"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.code").value("APPROVAL_ACTOR_REQUIRED"))
          .andExpect(jsonPath("$.traceId").exists());
    }
    mvc.perform(
            post("/api/v1/companies/1/financial-accounts/100/reject")
                .header("X-Actor-Id", "cannot-bypass")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"justification\":\"reason\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("APPROVAL_ACTOR_REQUIRED"))
        .andExpect(jsonPath("$.traceId").exists());
  }

  @Test
  void acceptsPaginationBoundariesAndRejectsValuesOutsideThem() throws Exception {
    when(list.execute(eq(1L), any())).thenReturn(new PageResult<>(List.of(), 0, 1, 0, 0));
    for (String query : List.of("page=0&size=1", "page=0&size=100"))
      mvc.perform(get("/api/v1/companies/1/financial-accounts?" + query))
          .andExpect(status().isOk());
    for (String query : List.of("page=-1", "size=0", "size=101"))
      expectValidation(get("/api/v1/companies/1/financial-accounts?" + query));
  }

  private void assertCreateError(RuntimeException exception, String code) throws Exception {
    reset(create);
    when(create.execute(any())).thenThrow(exception);
    mvc.perform(
            post("/api/v1/companies/1/financial-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("PAYABLE")))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value(code));
  }

  private void assertCreateNotFound(RuntimeException exception, String code) throws Exception {
    reset(create);
    when(create.execute(any())).thenThrow(exception);
    mvc.perform(
            post("/api/v1/companies/1/financial-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("PAYABLE")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(jsonPath("$.traceId").exists());
  }

  private void assertStructuralError(String message) throws Exception {
    reset(create);
    when(create.execute(any())).thenThrow(new InvalidFinancialAccountException(message));
    mvc.perform(
            post("/api/v1/companies/1/financial-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("PAYABLE")))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.traceId").exists());
  }

  private void expectValidation(
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
      throws Exception {
    mvc.perform(request)
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.traceId").exists());
  }

  private String body(String type) {
    return "{\"branchId\":2,\"type\":\""
        + type
        + "\",\"partnerId\":3,\"categoryId\":4,"
        + "\"costCenterId\":null,\"issueDate\":\"2026-08-21\",\"totalAmount\":150.00,"
        + "\"installments\":[{\"installmentNumber\":1,\"dueDate\":\"2026-09-10\",\"amount\":100.00},"
        + "{\"installmentNumber\":2,\"dueDate\":\"2026-10-10\",\"amount\":50.00}]}";
  }

  private FinancialAccount account(FinancialAccountType type) {
    return account(type, FinancialAccountStatus.DRAFT);
  }

  private FinancialAccount account(FinancialAccountType type, FinancialAccountStatus status) {
    return FinancialAccount.rehydrate(
        100L,
        1L,
        2L,
        type,
        3L,
        4L,
        null,
        LocalDate.of(2026, 8, 21),
        new BigDecimal("150.00"),
        status,
        List.of(
            Installment.rehydrate(1001L, 1, LocalDate.of(2026, 9, 10), new BigDecimal("100.00")),
            Installment.rehydrate(1002L, 2, LocalDate.of(2026, 10, 10), new BigDecimal("50.00"))));
  }

  private void assertApprovalError(
      Object useCase, RuntimeException exception, String action, int expectedStatus, String code)
      throws Exception {
    if (useCase == submitForApproval) {
      reset(submitForApproval);
      when(submitForApproval.execute(1L, 100L)).thenThrow(exception);
    } else {
      reset(approve);
      when(approve.execute(1L, 100L)).thenThrow(exception);
    }
    mvc.perform(post("/api/v1/companies/1/financial-accounts/100/" + action))
        .andExpect(status().is(expectedStatus))
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(jsonPath("$.traceId").exists());
  }

  private FinancialAccountSummary summary() {
    return new FinancialAccountSummary(
        100L,
        1L,
        2L,
        FinancialAccountType.PAYABLE,
        3L,
        4L,
        null,
        LocalDate.of(2026, 8, 21),
        new BigDecimal("150.00"),
        FinancialAccountStatus.DRAFT);
  }
}
