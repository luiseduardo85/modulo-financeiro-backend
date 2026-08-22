package com.financeiro.approval.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.financeiro.approval.application.*;
import com.financeiro.approval.domain.*;
import com.financeiro.category.application.CategoryRepository;
import com.financeiro.category.domain.Category;
import com.financeiro.company.application.*;
import com.financeiro.company.domain.*;
import com.financeiro.financialaccount.application.FinancialAccountRepository;
import com.financeiro.financialaccount.domain.*;
import com.financeiro.integration.support.PostgresIntegrationTestConfiguration;
import com.financeiro.partner.application.PartnerRepository;
import com.financeiro.partner.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("it")
@Import({
  PostgresIntegrationTestConfiguration.class,
  ApprovalWorkflowPersistenceIT.TrustedApprovalTestConfiguration.class
})
class ApprovalWorkflowPersistenceIT {
  private int fixtureSequence;
  @Autowired CompanyRepository companies;
  @Autowired BranchRepository branches;
  @Autowired PartnerRepository partners;
  @Autowired CategoryRepository categories;
  @Autowired FinancialAccountRepository accounts;
  @Autowired ApprovalConfigurationRepository configurations;
  @Autowired ApprovalRequestRepository requests;
  @Autowired ApprovalDecisionRepository decisions;
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired SubmitFinancialAccountForApproval submitForApproval;
  @Autowired ApproveFinancialAccount approveFinancialAccount;
  @Autowired RejectFinancialAccount rejectFinancialAccount;
  @Autowired ThreadLocalApprovalActorContext testActors;
  @Autowired BarrierApprovalEligibility testEligibility;

  @BeforeEach
  @AfterEach
  void clean() {
    jdbc.update("DELETE FROM \"financialMovement\"");
    fixtureSequence = 0;
    jdbc.execute("DROP TRIGGER IF EXISTS \"failApprovalRequestInsert\" ON \"approvalRequest\"");
    jdbc.execute("DROP TRIGGER IF EXISTS \"failApprovalDecisionInsert\" ON \"approvalDecision\"");
    testEligibility.disableBarrier();
    testActors.clear();
    jdbc.update("DELETE FROM \"approvalDecision\"");
    jdbc.update("DELETE FROM \"approvalRequest\"");
    jdbc.update("DELETE FROM \"approvalConfiguration\"");
    jdbc.update("DELETE FROM \"financialAccountHistory\"");
    jdbc.update("DELETE FROM \"installment\"");
    jdbc.update("DELETE FROM \"financialAccount\"");
    jdbc.update("DELETE FROM \"bankAccount\"");
    jdbc.update("DELETE FROM \"paymentMethod\"");
    jdbc.update("DELETE FROM \"costCenter\"");
    jdbc.update("DELETE FROM \"category\"");
    jdbc.update("DELETE FROM \"branch\"");
    jdbc.update("DELETE FROM \"partner\"");
    jdbc.update("DELETE FROM \"company\"");
  }

  @Test
  void v9AppliesAndVersionIncrementsOnManagedMutation() {
    var fixture = fixtures("Version");
    var saved = accounts.save(account(fixture));
    assertThat(version(saved.id())).isZero();

    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            ignored -> {
              var managed =
                  accounts.findByCompanyIdAndId(fixture.company().id(), saved.id()).orElseThrow();
              managed.approveWithoutWorkflow();
              accounts.updateStatus(managed);
            });

    assertThat(version(saved.id())).isOne();
    assertThat(
            jdbc.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version='9'", Boolean.class))
        .isTrue();
  }

  @Test
  void configurationLookupUsesBranchThenCompanyFallbackAndKeepsCompanyAndTypeIsolation() {
    var one = fixtures("Config one");
    var two = fixtures("Config two");
    configurations.save(
        ApprovalConfiguration.create(
            one.company().id(), null, FinancialAccountType.PAYABLE, false));
    configurations.save(
        ApprovalConfiguration.create(
            one.company().id(), one.branch().id(), FinancialAccountType.PAYABLE, true));
    configurations.save(
        ApprovalConfiguration.create(two.company().id(), null, FinancialAccountType.PAYABLE, true));

    assertThat(
            configurations
                .findApplicable(one.company().id(), one.branch().id(), FinancialAccountType.PAYABLE)
                .orElseThrow()
                .approvalRequired())
        .isTrue();
    assertThat(
            configurations
                .findApplicable(one.company().id(), 9999L, FinancialAccountType.PAYABLE)
                .orElseThrow()
                .approvalRequired())
        .isFalse();
    assertThat(
            configurations.findApplicable(
                one.company().id(), one.branch().id(), FinancialAccountType.RECEIVABLE))
        .isEmpty();
    assertThat(
            configurations.findApplicable(
                two.company().id(), two.branch().id(), FinancialAccountType.PAYABLE))
        .get()
        .extracting(ApprovalConfiguration::companyId)
        .isEqualTo(two.company().id());
  }

  @Test
  void partialIndexesBlockDuplicateCompanyWideAndBranchConfigurations() {
    var fixture = fixtures("Config uniqueness");
    configurations.save(
        ApprovalConfiguration.create(
            fixture.company().id(), null, FinancialAccountType.PAYABLE, true));
    assertThatThrownBy(
            () ->
                configurations.save(
                    ApprovalConfiguration.create(
                        fixture.company().id(), null, FinancialAccountType.PAYABLE, false)))
        .isInstanceOf(DataIntegrityViolationException.class);

    configurations.save(
        ApprovalConfiguration.create(
            fixture.company().id(), fixture.branch().id(), FinancialAccountType.RECEIVABLE, true));
    assertThatThrownBy(
            () ->
                configurations.save(
                    ApprovalConfiguration.create(
                        fixture.company().id(),
                        fixture.branch().id(),
                        FinancialAccountType.RECEIVABLE,
                        false)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void configurationForeignKeysProtectCompanyAndBranchWithoutCascade() {
    var fixture = fixtures("Config FK");
    configurations.save(
        ApprovalConfiguration.create(
            fixture.company().id(), fixture.branch().id(), FinancialAccountType.PAYABLE, true));
    assertThatThrownBy(
            () -> jdbc.update("DELETE FROM \"branch\" WHERE \"id\"=?", fixture.branch().id()))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () -> jdbc.update("DELETE FROM \"company\" WHERE \"id\"=?", fixture.company().id()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void requestAndDecisionPersistWithChecksUniquenessAndActorLengths() {
    var fixture = fixtures("Workflow records");
    var saved = accounts.save(account(fixture));
    var request = requests.save(ApprovalRequest.create(saved.id(), " requester "));
    var decision = decisions.save(ApprovalDecision.approve(request.id(), "approver"));
    assertThat(request.id()).isPositive();
    assertThat(decision.id()).isPositive();
    assertThat(requests.findPendingByFinancialAccountId(saved.id())).isPresent();

    assertThatThrownBy(() -> requests.save(ApprovalRequest.create(saved.id(), "other")))
        .isInstanceOf(ApprovalConflictException.class);
    assertThatThrownBy(() -> decisions.save(ApprovalDecision.approve(request.id(), "other")))
        .isInstanceOf(ApprovalConflictException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO \"approvalRequest\"(\"financialAccountId\",\"requesterActorId\",\"status\") VALUES(?,?,'INVALID')",
                    saved.id(),
                    "actor"))
        .isInstanceOf(DataIntegrityViolationException.class);
    Long rejectedRequestId =
        jdbc.queryForObject(
            "INSERT INTO \"approvalRequest\"(\"financialAccountId\",\"requesterActorId\",\"status\") VALUES(?,?,'REJECTED') RETURNING \"id\"",
            Long.class,
            saved.id(),
            "actor");
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO \"approvalDecision\"(\"approvalRequestId\",\"actorId\",\"decision\",\"rejectionJustification\") VALUES(?,?,'REJECTED',NULL)",
                    rejectedRequestId,
                    "actor"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO \"approvalRequest\"(\"financialAccountId\",\"requesterActorId\",\"status\") VALUES(?,?,'REJECTED')",
                    saved.id(),
                    "a".repeat(129)))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO \"approvalDecision\"(\"approvalRequestId\",\"actorId\",\"decision\",\"rejectionJustification\") VALUES(?,?,'INVALID',NULL)",
                    rejectedRequestId,
                    "actor"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO \"approvalDecision\"(\"approvalRequestId\",\"actorId\",\"decision\",\"rejectionJustification\") VALUES(?,?,'APPROVED','not allowed')",
                    rejectedRequestId,
                    "actor"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO \"approvalDecision\"(\"approvalRequestId\",\"actorId\",\"decision\",\"rejectionJustification\") VALUES(?,?,'APPROVED',NULL)",
                    rejectedRequestId,
                    "a".repeat(129)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void realSubmitUseCaseRollsBackAccountWhenRequestInsertFails() {
    var fixture = fixtures("Submit rollback");
    var saved = accounts.save(account(fixture));
    configurations.save(
        ApprovalConfiguration.create(
            fixture.company().id(), fixture.branch().id(), FinancialAccountType.PAYABLE, true));
    testActors.use("requester");
    installFailureTrigger("approvalRequest", "failApprovalRequestInsert");

    assertThatThrownBy(() -> submitForApproval.execute(fixture.company().id(), saved.id()))
        .isInstanceOf(RuntimeException.class);

    assertWorkflow(saved.id(), FinancialAccountStatus.DRAFT, null, 0, null, null);
  }

  @Test
  void realApproveUseCaseRollsBackCompleteWorkflowWhenDecisionInsertFails() {
    var pending = pendingWorkflow("Approve rollback");
    testActors.use("approver");
    installFailureTrigger("approvalDecision", "failApprovalDecisionInsert");

    assertThatThrownBy(
            () -> approveFinancialAccount.execute(pending.companyId(), pending.accountId()))
        .isInstanceOf(RuntimeException.class);

    assertWorkflow(
        pending.accountId(),
        FinancialAccountStatus.PENDING_APPROVAL,
        ApprovalRequestStatus.PENDING,
        0,
        null,
        null);
  }

  @Test
  void realRejectUseCaseRollsBackCompleteWorkflowWhenDecisionInsertFails() {
    var pending = pendingWorkflow("Reject rollback");
    testActors.use("rejector");
    installFailureTrigger("approvalDecision", "failApprovalDecisionInsert");

    assertThatThrownBy(
            () ->
                rejectFinancialAccount.execute(
                    pending.companyId(), pending.accountId(), "  valid reason  "))
        .isInstanceOf(RuntimeException.class);

    assertWorkflow(
        pending.accountId(),
        FinancialAccountStatus.PENDING_APPROVAL,
        ApprovalRequestStatus.PENDING,
        0,
        null,
        null);
  }

  @Test
  void concurrentRealApproveUseCasesCommitExactlyOneConsistentDecision() throws Exception {
    var pending = pendingWorkflow("Real approve race");
    var outcomes = runWorkflowRace(pending, true);

    assertOneWinner(outcomes);
    assertWorkflow(
        pending.accountId(),
        FinancialAccountStatus.APPROVED,
        ApprovalRequestStatus.APPROVED,
        1,
        ApprovalDecisionType.APPROVED,
        null);
  }

  @Test
  void concurrentRealApproveAndRejectCommitExactlyOneConsistentWorkflow() throws Exception {
    var pending = pendingWorkflow("Real decision race");
    var outcomes = runWorkflowRace(pending, false);

    assertOneWinner(outcomes);
    var accountStatus = accountStatus(pending.accountId());
    if (accountStatus == FinancialAccountStatus.APPROVED) {
      assertWorkflow(
          pending.accountId(),
          FinancialAccountStatus.APPROVED,
          ApprovalRequestStatus.APPROVED,
          1,
          ApprovalDecisionType.APPROVED,
          null);
    } else {
      assertWorkflow(
          pending.accountId(),
          FinancialAccountStatus.DRAFT,
          ApprovalRequestStatus.REJECTED,
          1,
          ApprovalDecisionType.REJECTED,
          "race reason");
    }
  }

  @Test
  void rejectedDecisionPersistsCanonicalJustificationAndDatabaseConsistency() {
    var fixture = fixtures("Rejected decision");
    var saved = accounts.save(account(fixture));
    var request = requests.save(ApprovalRequest.create(saved.id(), "requester"));
    var decision =
        decisions.save(
            ApprovalDecision.reject(
                request.id(), "rejector", new RejectionJustification("  missing invoice  ")));
    assertThat(decision.rejectionJustification()).isEqualTo("missing invoice");
    assertThat(
            jdbc.queryForObject(
                "SELECT \"rejectionJustification\" FROM \"approvalDecision\" WHERE \"id\"=?",
                String.class,
                decision.id()))
        .isEqualTo("missing invoice");
  }

  @Test
  void workflowRecordsPreventParentDeletionAndNeverCascade() {
    var fixture = fixtures("Workflow FK");
    var saved = accounts.save(account(fixture));
    var request = requests.save(ApprovalRequest.create(saved.id(), "requester"));
    decisions.save(ApprovalDecision.approve(request.id(), "approver"));
    assertThatThrownBy(
            () -> jdbc.update("DELETE FROM \"financialAccount\" WHERE \"id\"=?", saved.id()))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () -> jdbc.update("DELETE FROM \"approvalRequest\" WHERE \"id\"=?", request.id()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void decisionFailureRollsBackAccountRequestAndSiblingDecisionAtomically() {
    var fixture = fixtures("Atomic workflow");
    var saved = makePending(fixture);
    var request = requests.save(ApprovalRequest.create(saved.id(), "requester"));
    var transaction = new TransactionTemplate(transactionManager);

    assertThatThrownBy(
            () ->
                transaction.executeWithoutResult(
                    ignored -> {
                      var current =
                          accounts
                              .findByCompanyIdAndId(fixture.company().id(), saved.id())
                              .orElseThrow();
                      var pending =
                          requests.findPendingByFinancialAccountId(saved.id()).orElseThrow();
                      current.approve();
                      pending.approve();
                      accounts.updateStatus(current);
                      requests.save(pending);
                      decisions.save(ApprovalDecision.approve(request.id(), "one"));
                      decisions.save(ApprovalDecision.approve(request.id(), "two"));
                    }))
        .isInstanceOf(ApprovalConflictException.class);

    assertThat(
            accounts
                .findByCompanyIdAndId(fixture.company().id(), saved.id())
                .orElseThrow()
                .status())
        .isEqualTo(FinancialAccountStatus.PENDING_APPROVAL);
    assertThat(requests.findPendingByFinancialAccountId(saved.id())).isPresent();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM \"approvalDecision\"", Long.class))
        .isZero();
  }

  @Test
  void concurrentApproveVsApproveAllowsExactlyOneVersionedMutation() throws Exception {
    var fixture = fixtures("Approve race");
    var saved = makePending(fixture);
    assertRace(fixture, saved.id(), true);
    assertThat(
            accounts
                .findByCompanyIdAndId(fixture.company().id(), saved.id())
                .orElseThrow()
                .status())
        .isEqualTo(FinancialAccountStatus.APPROVED);
  }

  @Test
  void concurrentApproveVsRejectAllowsExactlyOneFinalState() throws Exception {
    var fixture = fixtures("Decision race");
    var saved = makePending(fixture);
    assertRace(fixture, saved.id(), false);
    assertThat(
            accounts
                .findByCompanyIdAndId(fixture.company().id(), saved.id())
                .orElseThrow()
                .status())
        .isIn(FinancialAccountStatus.APPROVED, FinancialAccountStatus.DRAFT);
  }

  private void assertRace(Fixture fixture, Long accountId, boolean bothApprove) throws Exception {
    var barrier = new CyclicBarrier(2);
    var successes = new AtomicInteger();
    var conflicts = new AtomicInteger();
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first =
          executor.submit(mutation(fixture, accountId, true, barrier, successes, conflicts));
      var second =
          executor.submit(mutation(fixture, accountId, bothApprove, barrier, successes, conflicts));
      first.get(20, TimeUnit.SECONDS);
      second.get(20, TimeUnit.SECONDS);
    }
    assertThat(successes).hasValue(1);
    assertThat(conflicts).hasValue(1);
  }

  private PendingWorkflow pendingWorkflow(String suffix) {
    var fixture = fixtures(suffix);
    var saved = accounts.save(account(fixture));
    configurations.save(
        ApprovalConfiguration.create(
            fixture.company().id(), fixture.branch().id(), FinancialAccountType.PAYABLE, true));
    testActors.use("requester");
    submitForApproval.execute(fixture.company().id(), saved.id());
    return new PendingWorkflow(fixture.company().id(), saved.id());
  }

  private List<Throwable> runWorkflowRace(PendingWorkflow pending, boolean bothApprove)
      throws Exception {
    testEligibility.enableBarrier(2);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(workflowAction(pending, "approver-b", true));
      var second = executor.submit(workflowAction(pending, "approver-c", bothApprove));
      return java.util.Arrays.asList(
          first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
    } finally {
      testEligibility.disableBarrier();
    }
  }

  private Callable<Throwable> workflowAction(
      PendingWorkflow pending, String actorId, boolean approve) {
    return () -> {
      testActors.use(actorId);
      try {
        if (approve) {
          approveFinancialAccount.execute(pending.companyId(), pending.accountId());
        } else {
          rejectFinancialAccount.execute(
              pending.companyId(), pending.accountId(), "  race reason  ");
        }
        return null;
      } catch (Throwable failure) {
        return failure;
      } finally {
        testActors.clear();
      }
    };
  }

  private static void assertOneWinner(List<Throwable> outcomes) {
    assertThat(outcomes).filteredOn(value -> value == null).hasSize(1);
    assertThat(outcomes)
        .filteredOn(value -> value != null)
        .singleElement()
        .isInstanceOfAny(
            ApprovalConflictException.class, InvalidFinancialAccountStatusException.class);
  }

  private void assertWorkflow(
      Long accountId,
      FinancialAccountStatus expectedAccount,
      ApprovalRequestStatus expectedRequest,
      int decisionCount,
      ApprovalDecisionType expectedDecision,
      String expectedJustification) {
    assertThat(accountStatus(accountId)).isEqualTo(expectedAccount);
    var requestsFound =
        jdbc.queryForList(
            "SELECT \"status\" FROM \"approvalRequest\" WHERE \"financialAccountId\"=? ORDER BY \"id\"",
            String.class,
            accountId);
    if (expectedRequest == null) {
      assertThat(requestsFound).isEmpty();
    } else {
      assertThat(requestsFound).containsExactly(expectedRequest.name());
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM \"approvalDecision\" d JOIN \"approvalRequest\" r ON r.\"id\"=d.\"approvalRequestId\" WHERE r.\"financialAccountId\"=?",
                Integer.class,
                accountId))
        .isEqualTo(decisionCount);
    if (decisionCount == 1) {
      var decision =
          jdbc.queryForMap(
              "SELECT d.\"decision\", d.\"rejectionJustification\" FROM \"approvalDecision\" d JOIN \"approvalRequest\" r ON r.\"id\"=d.\"approvalRequestId\" WHERE r.\"financialAccountId\"=?",
              accountId);
      assertThat(decision.get("decision")).isEqualTo(expectedDecision.name());
      assertThat(decision.get("rejectionJustification")).isEqualTo(expectedJustification);
    }
  }

  private FinancialAccountStatus accountStatus(Long accountId) {
    return FinancialAccountStatus.valueOf(
        jdbc.queryForObject(
            "SELECT \"status\" FROM \"financialAccount\" WHERE \"id\"=?", String.class, accountId));
  }

  private void installFailureTrigger(String table, String trigger) {
    jdbc.execute(
        "CREATE OR REPLACE FUNCTION \"failApprovalWorkflowInsert\"() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'forced workflow failure'; END $$");
    jdbc.execute(
        "CREATE TRIGGER \""
            + trigger
            + "\" BEFORE INSERT ON \""
            + table
            + "\" FOR EACH ROW EXECUTE FUNCTION \"failApprovalWorkflowInsert\"() ");
  }

  private Callable<Void> mutation(
      Fixture fixture,
      Long accountId,
      boolean approve,
      CyclicBarrier barrier,
      AtomicInteger successes,
      AtomicInteger conflicts) {
    return () -> {
      try {
        new TransactionTemplate(transactionManager)
            .executeWithoutResult(
                ignored -> {
                  var current =
                      accounts
                          .findByCompanyIdAndId(fixture.company().id(), accountId)
                          .orElseThrow();
                  await(barrier);
                  if (approve) current.approve();
                  else current.reject();
                  accounts.updateStatus(current);
                });
        successes.incrementAndGet();
      } catch (ApprovalConflictException exception) {
        conflicts.incrementAndGet();
      }
      return null;
    };
  }

  private static void await(CyclicBarrier barrier) {
    try {
      barrier.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(exception);
    } catch (BrokenBarrierException | TimeoutException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private FinancialAccount makePending(Fixture fixture) {
    var saved = accounts.save(account(fixture));
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            ignored -> {
              var current =
                  accounts.findByCompanyIdAndId(fixture.company().id(), saved.id()).orElseThrow();
              current.submitForApproval();
              accounts.updateStatus(current);
            });
    return accounts.findByCompanyIdAndId(fixture.company().id(), saved.id()).orElseThrow();
  }

  private long version(Long accountId) {
    return jdbc.queryForObject(
        "SELECT \"version\" FROM \"financialAccount\" WHERE \"id\"=?", Long.class, accountId);
  }

  private Fixture fixtures(String suffix) {
    var company = companies.save(Company.create("Company " + suffix));
    var branch = branches.save(Branch.create(company.id(), "Branch"));
    var partner =
        partners.save(
            Partner.create(
                "Partner " + suffix,
                Document.of(documentForNextFixture()),
                Set.of(PartnerRole.CUSTOMER, PartnerRole.SUPPLIER)));
    var category = categories.save(Category.create(company.id(), "Category"));
    return new Fixture(company, branch, partner, category);
  }

  private String documentForNextFixture() {
    return List.of("52998224725", "11144477735", "93541134780").get(fixtureSequence++);
  }

  private FinancialAccount account(Fixture fixture) {
    return FinancialAccount.create(
        fixture.company().id(),
        fixture.branch().id(),
        FinancialAccountType.PAYABLE,
        fixture.partner().id(),
        fixture.category().id(),
        null,
        LocalDate.of(2026, 8, 22),
        BigDecimal.TEN,
        List.of(Installment.create(1, LocalDate.of(2026, 9, 1), BigDecimal.TEN)));
  }

  private record Fixture(Company company, Branch branch, Partner partner, Category category) {}

  private record PendingWorkflow(Long companyId, Long accountId) {}

  @TestConfiguration(proxyBeanMethods = false)
  static class TrustedApprovalTestConfiguration {
    @Bean
    @Primary
    ThreadLocalApprovalActorContext testApprovalActorContext() {
      return new ThreadLocalApprovalActorContext();
    }

    @Bean
    @Primary
    BarrierApprovalEligibility testApprovalEligibility() {
      return new BarrierApprovalEligibility();
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

  static final class BarrierApprovalEligibility implements ApprovalEligibility {
    private volatile CyclicBarrier barrier;

    void enableBarrier(int parties) {
      barrier = new CyclicBarrier(parties);
    }

    void disableBarrier() {
      barrier = null;
    }

    @Override
    public boolean canApprove(String actorId, Long companyId) {
      var current = barrier;
      if (current != null) await(current);
      return true;
    }
  }
}
