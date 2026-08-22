package com.financeiro.cashflow.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.financeiro.approval.application.*;
import com.financeiro.cashflow.application.*;
import com.financeiro.cashflow.domain.CashFlowStatus;
import com.financeiro.category.application.CategoryRepository;
import com.financeiro.category.domain.Category;
import com.financeiro.company.application.*;
import com.financeiro.company.domain.*;
import com.financeiro.financialaccount.application.*;
import com.financeiro.financialaccount.domain.FinancialAccountType;
import com.financeiro.financialmovement.application.*;
import com.financeiro.integration.support.PostgresIntegrationTestConfiguration;
import com.financeiro.partner.application.PartnerRepository;
import com.financeiro.partner.domain.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("it")
@Import({
  PostgresIntegrationTestConfiguration.class,
  CashFlowPersistenceIT.TrustedApprovalTestConfiguration.class,
  CashFlowPersistenceIT.FixedClockTestConfiguration.class
})
class CashFlowPersistenceIT {
  private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 8, 1);
  private static final LocalDate TODAY = LocalDate.of(2026, 8, 18);

  @Autowired JdbcTemplate jdbc;
  @Autowired CompanyRepository companies;
  @Autowired BranchRepository branches;
  @Autowired PartnerRepository partners;
  @Autowired CategoryRepository categories;
  @Autowired CreateFinancialAccount create;
  @Autowired SubmitFinancialAccountForApproval submitForApproval;
  @Autowired SettleInstallment settle;
  @Autowired ReverseFinancialMovement reverse;
  @Autowired GetCashFlow getCashFlow;
  @Autowired ThreadLocalApprovalActorContext testActors;

  @BeforeEach
  @AfterEach
  void clean() {
    testActors.clear();
    jdbc.update("DELETE FROM \"financialMovement\"");
    jdbc.update("DELETE FROM \"idempotencyRecord\"");
    jdbc.update("DELETE FROM \"approvalDecision\"");
    jdbc.update("DELETE FROM \"approvalRequest\"");
    jdbc.update("DELETE FROM \"approvalConfiguration\"");
    jdbc.update("DELETE FROM \"financialAccountHistory\"");
    jdbc.update("DELETE FROM \"installment\"");
    jdbc.update("DELETE FROM \"financialAccount\"");
    jdbc.update("DELETE FROM \"bankAccount\"");
    jdbc.update("DELETE FROM \"paymentMethod\"");
    jdbc.update("DELETE FROM \"category\"");
    jdbc.update("DELETE FROM \"branch\"");
    jdbc.update("DELETE FROM \"partner\"");
    jdbc.update("DELETE FROM \"company\"");
  }

  @Test
  void forecastSignsByAccountTypeAndDerivesOverdueFromCurrentDate() {
    var fixture = fixture("Signs");
    approvedAccount(fixture, FinancialAccountType.PAYABLE, LocalDate.of(2026, 8, 10), "100.00");
    approvedAccount(fixture, FinancialAccountType.RECEIVABLE, LocalDate.of(2026, 8, 15), "200.00");
    approvedAccount(fixture, FinancialAccountType.PAYABLE, LocalDate.of(2026, 8, 20), "50.00");

    var projection =
        getCashFlow.execute(
            new CashFlowQuery(
                fixture.companyId(), null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)));

    assertThat(projection.days()).hasSize(3);
    var day10 = dayOn(projection, LocalDate.of(2026, 8, 10));
    assertThat(day10.forecastOutflow()).isEqualByComparingTo("100.00");
    assertThat(day10.forecastInflow()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(day10.status()).isEqualTo(CashFlowStatus.OVERDUE);

    var day15 = dayOn(projection, LocalDate.of(2026, 8, 15));
    assertThat(day15.forecastInflow()).isEqualByComparingTo("200.00");
    assertThat(day15.status()).isEqualTo(CashFlowStatus.OVERDUE);

    var day20 = dayOn(projection, LocalDate.of(2026, 8, 20));
    assertThat(day20.forecastOutflow()).isEqualByComparingTo("50.00");
    assertThat(day20.status()).isEqualTo(CashFlowStatus.SCHEDULED);

    assertThat(projection.summary().totalForecastOutflow()).isEqualByComparingTo("150.00");
    assertThat(projection.summary().totalForecastInflow()).isEqualByComparingTo("200.00");
  }

  @Test
  void actualMovementsNetEffectAndReversalFlipsSign() {
    var fixture = fixture("Realized");
    var payable =
        approvedAccount(fixture, FinancialAccountType.PAYABLE, LocalDate.of(2026, 8, 10), "100.00");
    var receivable =
        approvedAccount(
            fixture, FinancialAccountType.RECEIVABLE, LocalDate.of(2026, 8, 15), "200.00");

    var payment =
        settleInstallment(
            fixture,
            payable.id(),
            payable.installments().getFirst().id(),
            "100.00",
            LocalDate.of(2026, 8, 12));
    reverseMovement(
        fixture,
        payable.id(),
        payable.installments().getFirst().id(),
        payment.id(),
        "40.00",
        LocalDate.of(2026, 8, 13));
    var receipt =
        settleInstallment(
            fixture,
            receivable.id(),
            receivable.installments().getFirst().id(),
            "200.00",
            LocalDate.of(2026, 8, 16));
    reverseMovement(
        fixture,
        receivable.id(),
        receivable.installments().getFirst().id(),
        receipt.id(),
        "50.00",
        LocalDate.of(2026, 8, 17));

    var projection =
        getCashFlow.execute(
            new CashFlowQuery(
                fixture.companyId(), null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)));

    assertThat(dayOn(projection, LocalDate.of(2026, 8, 12)).actualOutflow())
        .isEqualByComparingTo("100.00");
    assertThat(dayOn(projection, LocalDate.of(2026, 8, 13)).actualInflow())
        .isEqualByComparingTo("40.00");
    assertThat(dayOn(projection, LocalDate.of(2026, 8, 16)).actualInflow())
        .isEqualByComparingTo("200.00");
    assertThat(dayOn(projection, LocalDate.of(2026, 8, 17)).actualOutflow())
        .isEqualByComparingTo("50.00");

    assertThat(projection.summary().totalActualOutflow()).isEqualByComparingTo("150.00");
    assertThat(projection.summary().totalActualInflow()).isEqualByComparingTo("240.00");
  }

  @Test
  void partialSettlementLeavesOnlyRemainingBalanceInForecast() {
    var fixture = fixture("Partial");
    var account =
        approvedAccount(fixture, FinancialAccountType.PAYABLE, LocalDate.of(2026, 8, 25), "100.00");
    settleInstallment(
        fixture,
        account.id(),
        account.installments().getFirst().id(),
        "40.00",
        LocalDate.of(2026, 8, 19));

    var projection =
        getCashFlow.execute(
            new CashFlowQuery(
                fixture.companyId(), null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)));

    assertThat(dayOn(projection, LocalDate.of(2026, 8, 25)).forecastOutflow())
        .isEqualByComparingTo("60.00");
    assertThat(dayOn(projection, LocalDate.of(2026, 8, 19)).actualOutflow())
        .isEqualByComparingTo("40.00");

    settleInstallment(
        fixture,
        account.id(),
        account.installments().getFirst().id(),
        "60.00",
        LocalDate.of(2026, 8, 26));

    var afterFullSettlement =
        getCashFlow.execute(
            new CashFlowQuery(
                fixture.companyId(), null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)));

    assertThat(afterFullSettlement.days())
        .noneMatch(day -> day.date().equals(LocalDate.of(2026, 8, 25)));
    assertThat(dayOn(afterFullSettlement, LocalDate.of(2026, 8, 26)).actualOutflow())
        .isEqualByComparingTo("60.00");
  }

  @Test
  void draftAccountsAreExcludedFromForecastUntilApproved() {
    var fixture = fixture("Draft");
    var command =
        new CreateFinancialAccountCommand(
            fixture.companyId(),
            fixture.branchId(),
            FinancialAccountType.PAYABLE,
            fixture.partnerId(),
            fixture.categoryId(),
            null,
            ISSUE_DATE,
            new BigDecimal("100.00"),
            List.of(
                new CreateInstallmentCommand(
                    1, LocalDate.of(2026, 8, 20), new BigDecimal("100.00"))));
    create.execute(command);

    var projection =
        getCashFlow.execute(
            new CashFlowQuery(
                fixture.companyId(), null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)));

    assertThat(projection.days()).isEmpty();
  }

  @Test
  void datesOutsideTheQueryRangeAreExcludedFromForecastAndActual() {
    var fixture = fixture("Range");
    var account =
        approvedAccount(fixture, FinancialAccountType.PAYABLE, LocalDate.of(2026, 9, 5), "100.00");
    settleInstallment(
        fixture,
        account.id(),
        account.installments().getFirst().id(),
        "100.00",
        LocalDate.of(2026, 7, 20));

    var projection =
        getCashFlow.execute(
            new CashFlowQuery(
                fixture.companyId(), null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)));

    assertThat(projection.days()).isEmpty();
  }

  @Test
  void cashFlowNeverLeaksAcrossCompanies() {
    var companyA = fixture("Company A");
    var companyB = fixture("Company B");
    approvedAccount(companyA, FinancialAccountType.PAYABLE, LocalDate.of(2026, 8, 10), "100.00");
    approvedAccount(companyB, FinancialAccountType.PAYABLE, LocalDate.of(2026, 8, 10), "999.00");

    var projection =
        getCashFlow.execute(
            new CashFlowQuery(
                companyA.companyId(), null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)));

    assertThat(projection.days()).hasSize(1);
    assertThat(projection.days().getFirst().forecastOutflow()).isEqualByComparingTo("100.00");
  }

  @Test
  void branchFilterScopesForecastAndActualToThatBranchOnly() {
    var fixture = fixture("Branch");
    var otherBranch = branches.save(Branch.create(fixture.companyId(), "Second Branch"));
    var otherFixture =
        new Fixture(
            fixture.companyId(),
            otherBranch.id(),
            fixture.partnerId(),
            fixture.categoryId(),
            fixture.bankAccountId(),
            fixture.paymentMethodId());

    approvedAccount(fixture, FinancialAccountType.PAYABLE, LocalDate.of(2026, 8, 10), "100.00");
    approvedAccount(otherFixture, FinancialAccountType.PAYABLE, LocalDate.of(2026, 8, 10), "70.00");

    var scoped =
        getCashFlow.execute(
            new CashFlowQuery(
                fixture.companyId(),
                fixture.branchId(),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)));
    var combined =
        getCashFlow.execute(
            new CashFlowQuery(
                fixture.companyId(), null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)));

    assertThat(scoped.days().getFirst().forecastOutflow()).isEqualByComparingTo("100.00");
    assertThat(combined.days().getFirst().forecastOutflow()).isEqualByComparingTo("170.00");
  }

  @Test
  void emptyStateReturnsNoDaysAndZeroSummary() {
    var fixture = fixture("Empty");

    var projection =
        getCashFlow.execute(
            new CashFlowQuery(
                fixture.companyId(), null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)));

    assertThat(projection.days()).isEmpty();
    assertThat(projection.summary().totalForecastNet()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(projection.summary().totalActualNet()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void sqlAggregatesMultipleInstallmentsAndMovementsIntoASingleDayBucket() {
    var fixture = fixture("Aggregate");
    approvedAccount(fixture, FinancialAccountType.PAYABLE, LocalDate.of(2026, 8, 20), "30.00");
    approvedAccount(fixture, FinancialAccountType.PAYABLE, LocalDate.of(2026, 8, 20), "70.00");
    var settled1 =
        approvedAccount(fixture, FinancialAccountType.PAYABLE, LocalDate.of(2026, 8, 5), "40.00");
    var settled2 =
        approvedAccount(fixture, FinancialAccountType.PAYABLE, LocalDate.of(2026, 8, 6), "60.00");
    settleInstallment(
        fixture,
        settled1.id(),
        settled1.installments().getFirst().id(),
        "40.00",
        LocalDate.of(2026, 8, 21));
    settleInstallment(
        fixture,
        settled2.id(),
        settled2.installments().getFirst().id(),
        "60.00",
        LocalDate.of(2026, 8, 21));

    var projection =
        getCashFlow.execute(
            new CashFlowQuery(
                fixture.companyId(), null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)));

    assertThat(dayOn(projection, LocalDate.of(2026, 8, 20)).forecastOutflow())
        .isEqualByComparingTo("100.00");
    assertThat(dayOn(projection, LocalDate.of(2026, 8, 21)).actualOutflow())
        .isEqualByComparingTo("100.00");
  }

  private CashFlowDay dayOn(CashFlowProjection projection, LocalDate date) {
    return projection.days().stream()
        .filter(day -> day.date().equals(date))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No cash flow day for " + date));
  }

  private com.financeiro.financialaccount.domain.FinancialAccount approvedAccount(
      Fixture fixture, FinancialAccountType type, LocalDate dueDate, String amount) {
    var command =
        new CreateFinancialAccountCommand(
            fixture.companyId(),
            fixture.branchId(),
            type,
            fixture.partnerId(),
            fixture.categoryId(),
            null,
            ISSUE_DATE,
            new BigDecimal(amount),
            List.of(new CreateInstallmentCommand(1, dueDate, new BigDecimal(amount))));
    var saved = create.execute(command);
    testActors.use("actor-" + saved.id());
    return submitForApproval.execute(fixture.companyId(), saved.id());
  }

  private SettlementResult settleInstallment(
      Fixture fixture, Long accountId, Long installmentId, String amount, LocalDate movementDate) {
    return settle.execute(
        new SettleInstallmentCommand(
            fixture.companyId(),
            accountId,
            installmentId,
            new BigDecimal(amount),
            movementDate,
            fixture.bankAccountId(),
            fixture.paymentMethodId(),
            "settle-" + accountId + "-" + installmentId + "-" + movementDate + "-" + amount));
  }

  private ReversalResult reverseMovement(
      Fixture fixture,
      Long accountId,
      Long installmentId,
      Long movementId,
      String amount,
      LocalDate movementDate) {
    return reverse.execute(
        new ReverseFinancialMovementCommand(
            fixture.companyId(),
            accountId,
            installmentId,
            movementId,
            new BigDecimal(amount),
            movementDate,
            fixture.bankAccountId(),
            fixture.paymentMethodId(),
            "reverse-" + movementId + "-" + movementDate + "-" + amount));
  }

  private static final List<String> FIXTURE_DOCUMENTS =
      List.of("52998224725", "11144477735", "93541134780", "72564930075");
  private int fixtureSequence = 0;

  private Fixture fixture(String suffix) {
    var company = companies.save(Company.create("Cash Flow Company " + suffix));
    var branch = branches.save(Branch.create(company.id(), "Cash Flow Branch"));
    var partner =
        partners.save(
            Partner.create(
                "Cash Flow Partner " + suffix,
                Document.of(FIXTURE_DOCUMENTS.get(fixtureSequence++)),
                Set.of(PartnerRole.SUPPLIER, PartnerRole.CUSTOMER)));
    var category = categories.save(Category.create(company.id(), "Cash Flow Category"));
    Long bank =
        jdbc.queryForObject(
            "INSERT INTO \"bankAccount\"(\"companyId\",\"branchId\",\"name\",\"active\") VALUES(?,NULL,'Bank',TRUE) RETURNING \"id\"",
            Long.class,
            company.id());
    Long payment =
        jdbc.queryForObject(
            "INSERT INTO \"paymentMethod\"(\"companyId\",\"name\",\"active\") VALUES(?,'PIX',TRUE) RETURNING \"id\"",
            Long.class,
            company.id());
    return new Fixture(company.id(), branch.id(), partner.id(), category.id(), bank, payment);
  }

  private record Fixture(
      Long companyId,
      Long branchId,
      Long partnerId,
      Long categoryId,
      Long bankAccountId,
      Long paymentMethodId) {}

  @TestConfiguration(proxyBeanMethods = false)
  static class TrustedApprovalTestConfiguration {
    @Bean
    @Primary
    ThreadLocalApprovalActorContext testApprovalActorContext() {
      return new ThreadLocalApprovalActorContext();
    }

    @Bean
    @Primary
    ApprovalEligibility testApprovalEligibility() {
      return (actorId, companyId) -> true;
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class FixedClockTestConfiguration {
    @Bean
    @Primary
    Clock fixedClock() {
      return Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    }
  }

  static final class ThreadLocalApprovalActorContext implements ApprovalActorContext {
    private final ThreadLocal<String> actors = new ThreadLocal<>();

    void use(String actorId) {
      actors.set(actorId);
    }

    void clear() {
      actors.remove();
    }

    @Override
    public ApprovalActor currentActor() {
      var actor = actors.get();
      if (actor == null) throw new ApprovalActorUnavailableException();
      return new ApprovalActor(actor);
    }
  }
}
