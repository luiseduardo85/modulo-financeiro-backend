package com.financeiro;

import com.financeiro.idempotency.application.IdempotencyStore;
import com.financeiro.company.application.BranchRepository;
import com.financeiro.company.application.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class FinanceiroBackendApplicationTest {

    @MockitoBean
    private IdempotencyStore idempotencyStore;

    @MockitoBean
    private CompanyRepository companyRepository;

    @MockitoBean
    private BranchRepository branchRepository;

    @Test
    void contextLoads() {
        // Verifies the Spring application context starts successfully
        // with no additional infrastructure (no DB, no JPA, no Flyway).
    }

}
