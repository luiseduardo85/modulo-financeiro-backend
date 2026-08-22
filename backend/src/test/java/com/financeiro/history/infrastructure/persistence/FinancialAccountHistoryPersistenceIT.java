package com.financeiro.history.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.financeiro.approval.application.*;
import com.financeiro.approval.domain.ApprovalConfiguration;
import com.financeiro.category.application.CategoryRepository;
import com.financeiro.category.domain.Category;
import com.financeiro.company.application.*;
import com.financeiro.company.domain.*;
import com.financeiro.financialaccount.application.*;
import com.financeiro.financialaccount.domain.FinancialAccountType;
import com.financeiro.financialmovement.application.*;
import com.financeiro.financialmovement.domain.FinancialMovementType;
import com.financeiro.history.application.*;
import com.financeiro.integration.support.PostgresIntegrationTestConfiguration;
import com.financeiro.partner.application.PartnerRepository;
import com.financeiro.partner.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;
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
  FinancialAccountHistoryPersistenceIT.TrustedApprovalTestConfiguration.class
})
class FinancialAccountHistoryPersistenceIT {
  private static final LocalDate MOVEMENT_DATE = LocalDate.of(2026, 8, 22);
  @Autowired JdbcTemplate jdbc;
  @Autowired CompanyRepository companies;
  @Autowired BranchRepository branches;
  @Autowired PartnerRepository partners;
  @Autowired CategoryRepository categories;
  @Autowired ApprovalConfigurationRepository configurations;
  @Autowired CreateFinancialAccount create;
  @Autowired SubmitFinancialAccountForApproval submitForApproval;
  @Autowired ApproveFinancialAccount approve;
  @Autowired RejectFinancialAccount reject;
  @Autowired SettleInstallment settle;
  @Autowired ReverseFinancialMovement reverse;
  @Autowired GetFinancialAccountHistory history;
  @Autowired ThreadLocalApprovalActorContext testActors;

  @BeforeEach
  @AfterEach
  void clean() {
    testActors.clear();
    jdbc.execute(
        "DROP TRIGGER IF EXISTS \"failFinancialAccountHistoryInsert\" ON \"financialAccountHistory\"");
    jdbc.execute("DROP FUNCTION IF EXISTS \"failFinancialAccountHistoryInsertFunction\"() ");
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
  void v12MigrationAppliesAndCreationProducesHistoryEntry() {
    var fixture = fixture("Creation");
    var saved = create.execute(command(fixture, List.of(new BigDecimal("100.00"))));

    var timeline = history.execute(fixture.companyId(), saved.id());

    assertThat(timeline).hasSize(1);
    assertThat(timeline.getFirst().type()).isEqualTo(TimelineEntryType.CREATED);
    assertThat(timeline.getFirst().actorId()).isNull();
    assertThat(timeline.getFirst().occurredAt()).isNotNull();
    assertThat(
            jdbc.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version='12'", Boolean.class))
        .isTrue();
  }

  @Test
  void directApprovalWithoutWorkflowRecordsHistoryEntryWithActor() {
    var fixture = fixture("Direct approval");
    var saved = create.execute(command(fixture, List.of(new BigDecimal("100.00"))));
    testActors.use("requester-1");

    submitForApproval.execute(fixture.companyId(), saved.id());

    var timeline = history.execute(fixture.companyId(), saved.id());
    assertThat(timeline)
        .extracting(TimelineEntry::type)
        .containsExactly(TimelineEntryType.CREATED, TimelineEntryType.APPROVED_WITHOUT_WORKFLOW);
    assertThat(timeline.get(1).actorId()).isEqualTo("requester-1");
    assertThat(timeline.get(0).occurredAt()).isBeforeOrEqualTo(timeline.get(1).occurredAt());
  }

  @Test
  void fullWorkflowRejectionThenApprovalProducesCompleteOrderedTimeline() {
    var fixture = fixture("Full workflow");
    configurations.save(
        ApprovalConfiguration.create(
            fixture.companyId(), fixture.branchId(), FinancialAccountType.PAYABLE, true));
    var saved = create.execute(command(fixture, List.of(new BigDecimal("100.00"))));

    testActors.use("requester-2");
    submitForApproval.execute(fixture.companyId(), saved.id());
    testActors.use("rejector-2");
    reject.execute(fixture.companyId(), saved.id(), "missing invoice");
    testActors.use("requester-2");
    submitForApproval.execute(fixture.companyId(), saved.id());
    testActors.use("approver-2");
    approve.execute(fixture.companyId(), saved.id());

    var timeline = history.execute(fixture.companyId(), saved.id());
    assertThat(timeline)
        .extracting(TimelineEntry::type)
        .containsExactly(
            TimelineEntryType.CREATED,
            TimelineEntryType.SUBMITTED_FOR_APPROVAL,
            TimelineEntryType.APPROVAL_DECISION,
            TimelineEntryType.SUBMITTED_FOR_APPROVAL,
            TimelineEntryType.APPROVAL_DECISION);
    assertThat(timeline.get(2).rejectionJustification()).isEqualTo("missing invoice");
    assertThat(timeline.get(4).rejectionJustification()).isNull();
    for (int i = 1; i < timeline.size(); i++) {
      assertThat(timeline.get(i - 1).occurredAt()).isBeforeOrEqualTo(timeline.get(i).occurredAt());
    }
  }

  @Test
  void settlementAndReversalAppearAfterApprovalInCorrectOrder() {
    var fixture = fixture("Settlement and reversal");
    var saved = create.execute(command(fixture, List.of(new BigDecimal("100.00"))));
    testActors.use("requester-3");
    submitForApproval.execute(fixture.companyId(), saved.id());
    Long installmentId = saved.installments().getFirst().id();

    var settled =
        settle.execute(
            new SettleInstallmentCommand(
                fixture.companyId(),
                saved.id(),
                installmentId,
                new BigDecimal("100.00"),
                MOVEMENT_DATE,
                fixture.bankAccountId(),
                fixture.paymentMethodId(),
                "settle-key"));
    var reversed =
        reverse.execute(
            new ReverseFinancialMovementCommand(
                fixture.companyId(),
                saved.id(),
                installmentId,
                settled.id(),
                new BigDecimal("40.00"),
                MOVEMENT_DATE,
                fixture.bankAccountId(),
                fixture.paymentMethodId(),
                "reverse-key"));

    var timeline = history.execute(fixture.companyId(), saved.id());
    assertThat(timeline)
        .extracting(TimelineEntry::type)
        .containsExactly(
            TimelineEntryType.CREATED,
            TimelineEntryType.APPROVED_WITHOUT_WORKFLOW,
            TimelineEntryType.FINANCIAL_MOVEMENT,
            TimelineEntryType.FINANCIAL_MOVEMENT);
    var payment = timeline.get(2);
    assertThat(payment.movementId()).isEqualTo(settled.id());
    assertThat(payment.movementType()).isEqualTo(FinancialMovementType.PAYMENT);
    assertThat(payment.amount()).isEqualByComparingTo("100.00");
    assertThat(payment.movementDate()).isEqualTo(MOVEMENT_DATE);
    assertThat(payment.originalMovementId()).isNull();
    var reversal = timeline.get(3);
    assertThat(reversal.movementId()).isEqualTo(reversed.id());
    assertThat(reversal.movementType()).isEqualTo(FinancialMovementType.REVERSAL_PAYMENT);
    assertThat(reversal.originalMovementId()).isEqualTo(settled.id());
  }

  @Test
  void historyIsScopedByCompanyAndNeverLeaksAcrossCompanies() {
    var fixture = fixture("Scope");
    var saved = create.execute(command(fixture, List.of(new BigDecimal("100.00"))));

    assertThat(history.execute(fixture.companyId(), saved.id())).hasSize(1);
    assertThatThrownBy(() -> history.execute(fixture.companyId() + 999, saved.id()))
        .isInstanceOf(FinancialAccountNotFoundException.class);
  }

  @Test
  void creationRollsBackAtomicallyWhenHistoryInsertFails() {
    var fixture = fixture("Creation rollback");
    installFinancialAccountHistoryInsertFailure();

    assertThatThrownBy(() -> create.execute(command(fixture, List.of(new BigDecimal("100.00")))))
        .isInstanceOf(RuntimeException.class);

    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM \"financialAccount\" WHERE \"companyId\"=?",
                Long.class,
                fixture.companyId()))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM \"financialAccountHistory\"", Long.class))
        .isZero();
  }

  @Test
  void directApprovalRollsBackAtomicallyWhenHistoryInsertFails() {
    var fixture = fixture("Approval rollback");
    var saved = create.execute(command(fixture, List.of(new BigDecimal("100.00"))));
    testActors.use("requester-4");
    installFinancialAccountHistoryInsertFailure();

    assertThatThrownBy(() -> submitForApproval.execute(fixture.companyId(), saved.id()))
        .isInstanceOf(RuntimeException.class);

    assertThat(
            jdbc.queryForObject(
                "SELECT \"status\" FROM \"financialAccount\" WHERE \"id\"=?",
                String.class,
                saved.id()))
        .isEqualTo("DRAFT");
    assertThat(
            jdbc.queryForObject(
                "SELECT \"version\" FROM \"financialAccount\" WHERE \"id\"=?",
                Long.class,
                saved.id()))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM \"financialAccountHistory\"", Long.class))
        .isEqualTo(1);
  }

  private void installFinancialAccountHistoryInsertFailure() {
    jdbc.execute(
        "CREATE FUNCTION \"failFinancialAccountHistoryInsertFunction\"() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'forced financial account history insert failure'; END $$");
    jdbc.execute(
        "CREATE TRIGGER \"failFinancialAccountHistoryInsert\" BEFORE INSERT ON \"financialAccountHistory\" FOR EACH ROW EXECUTE FUNCTION \"failFinancialAccountHistoryInsertFunction\"() ");
  }

  private CreateFinancialAccountCommand command(Fixture fixture, List<BigDecimal> installments) {
    BigDecimal total = installments.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    var items = new java.util.ArrayList<CreateInstallmentCommand>();
    for (int index = 0; index < installments.size(); index++) {
      items.add(
          new CreateInstallmentCommand(
              index + 1, LocalDate.of(2026, 9, 1).plusMonths(index), installments.get(index)));
    }
    return new CreateFinancialAccountCommand(
        fixture.companyId(),
        fixture.branchId(),
        FinancialAccountType.PAYABLE,
        fixture.partnerId(),
        fixture.categoryId(),
        null,
        LocalDate.of(2026, 8, 22),
        total,
        items);
  }

  private Fixture fixture(String suffix) {
    var company = companies.save(Company.create("History Company " + suffix));
    var branch = branches.save(Branch.create(company.id(), "History Branch"));
    var partner =
        partners.save(
            Partner.create(
                "History Partner " + suffix,
                Document.of("52998224725"),
                Set.of(PartnerRole.SUPPLIER, PartnerRole.CUSTOMER)));
    var category = categories.save(Category.create(company.id(), "History Category"));
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
