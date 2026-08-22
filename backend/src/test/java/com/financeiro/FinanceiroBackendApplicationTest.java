package com.financeiro;

import com.financeiro.bankaccount.application.BankAccountRepository;
import com.financeiro.category.application.CategoryRepository;
import com.financeiro.company.application.BranchRepository;
import com.financeiro.company.application.CompanyRepository;
import com.financeiro.costcenter.application.CostCenterRepository;
import com.financeiro.idempotency.application.IdempotencyStore;
import com.financeiro.partner.application.PartnerRepository;
import com.financeiro.paymentmethod.application.PaymentMethodRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class FinanceiroBackendApplicationTest {

  @MockitoBean private IdempotencyStore idempotencyStore;

  @MockitoBean private CompanyRepository companyRepository;

  @MockitoBean private BranchRepository branchRepository;

  @MockitoBean private PartnerRepository partnerRepository;

  @MockitoBean private CategoryRepository categoryRepository;

  @MockitoBean private CostCenterRepository costCenterRepository;

  @MockitoBean private BankAccountRepository bankAccountRepository;

  @MockitoBean private PaymentMethodRepository paymentMethodRepository;

  @Test
  void contextLoads() {
    // Verifies the Spring application context starts successfully
    // with no additional infrastructure (no DB, no JPA, no Flyway).
  }
}
