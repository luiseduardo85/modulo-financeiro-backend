package com.financeiro;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FinanceiroBackendApplicationTest {

    @Test
    void contextLoads() {
        // Verifies the Spring application context starts successfully
        // with no additional infrastructure (no DB, no JPA, no Flyway).
    }

}
