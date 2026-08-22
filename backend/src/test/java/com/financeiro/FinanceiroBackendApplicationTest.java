package com.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financeiro.approval.application.ApprovalActorContext;
import com.financeiro.approval.application.ApprovalActorUnavailableException;
import com.financeiro.approval.application.ApprovalConfigurationRepository;
import com.financeiro.approval.application.ApprovalDecisionRepository;
import com.financeiro.approval.application.ApprovalEligibility;
import com.financeiro.approval.application.ApprovalRequestRepository;
import com.financeiro.bankaccount.application.BankAccountRepository;
import com.financeiro.category.application.CategoryRepository;
import com.financeiro.company.application.BranchRepository;
import com.financeiro.company.application.CompanyRepository;
import com.financeiro.costcenter.application.CostCenterRepository;
import com.financeiro.financialaccount.application.FinancialAccountRepository;
import com.financeiro.idempotency.application.IdempotencyStore;
import com.financeiro.partner.application.PartnerRepository;
import com.financeiro.paymentmethod.application.PaymentMethodRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class FinanceiroBackendApplicationTest {

  @Autowired private ApprovalActorContext approvalActorContext;

  @Autowired private ApprovalEligibility approvalEligibility;

  @MockitoBean private IdempotencyStore idempotencyStore;

  @MockitoBean private CompanyRepository companyRepository;

  @MockitoBean private BranchRepository branchRepository;

  @MockitoBean private PartnerRepository partnerRepository;

  @MockitoBean private CategoryRepository categoryRepository;

  @MockitoBean private CostCenterRepository costCenterRepository;

  @MockitoBean private BankAccountRepository bankAccountRepository;

  @MockitoBean private PaymentMethodRepository paymentMethodRepository;

  @MockitoBean private FinancialAccountRepository financialAccountRepository;

  @MockitoBean private ApprovalConfigurationRepository approvalConfigurationRepository;

  @MockitoBean private ApprovalRequestRepository approvalRequestRepository;

  @MockitoBean private ApprovalDecisionRepository approvalDecisionRepository;

  @Test
  void contextLoads() {
    // Verifies the Spring application context starts successfully
    // with no additional infrastructure (no DB, no JPA, no Flyway).
  }

  @Test
  void productionApprovalBoundariesFailClosedUntilTrustedAdaptersExist() {
    assertThatThrownBy(approvalActorContext::currentActor)
        .isInstanceOf(ApprovalActorUnavailableException.class);
    assertThat(approvalEligibility.canApprove("any-actor", 1L)).isFalse();
  }
}
