package com.financeiro.financialmovement.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.financeiro.company.application.PageResult;
import com.financeiro.financialaccount.application.*;
import com.financeiro.financialaccount.domain.FinancialAccount;
import com.financeiro.financialmovement.application.*;
import com.financeiro.financialmovement.domain.FinancialMovementType;
import com.financeiro.integration.support.PostgresIntegrationTestConfiguration;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("it")
@Import({
  PostgresIntegrationTestConfiguration.class,
  FinancialMovementPersistenceIT.ConcurrencyConfiguration.class
})
class FinancialMovementPersistenceIT {
  private static final LocalDate MOVEMENT_DATE = LocalDate.of(2026, 8, 22);
  @Autowired JdbcTemplate jdbc;
  @Autowired SettleInstallment settle;
  @Autowired FinancialMovementRepository movements;
  @Autowired ObservedFinancialMovementRepository observedMovements;
  @Autowired SettlementBalanceRepository balances;
  @Autowired CoordinatedFinancialAccountRepository coordinatedAccounts;

  @BeforeEach
  @AfterEach
  void clean() {
    coordinatedAccounts.disarm();
    observedMovements.reset();
    jdbc.execute("DROP TRIGGER IF EXISTS \"failFinancialMovementInsert\" ON \"financialMovement\"");
    jdbc.execute(
        "DROP TRIGGER IF EXISTS \"failFinancialAccountSettlementUpdate\" ON \"financialAccount\"");
    jdbc.execute("DROP FUNCTION IF EXISTS \"failFinancialMovementInsertFunction\"() ");
    jdbc.execute("DROP FUNCTION IF EXISTS \"failFinancialAccountSettlementUpdateFunction\"() ");
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
    jdbc.update("DELETE FROM \"costCenter\"");
    jdbc.update("DELETE FROM \"category\"");
    jdbc.update("DELETE FROM \"branch\"");
    jdbc.update("DELETE FROM \"partner\"");
    jdbc.update("DELETE FROM \"company\"");
  }

  @Test
  void v10MappingPersistsPaymentMoneyDateAndScopedReplayWhileIncrementingVersion() {
    Fixture fixture = fixture("PAYABLE", List.of(new BigDecimal("100.00")));

    SettlementResult result = settle.execute(command(fixture, 0, "40.00", "key-1"));

    assertThat(result.type()).isEqualTo(FinancialMovementType.PAYMENT);
    assertThat(result.amount()).isEqualByComparingTo("40.00");
    assertThat(result.movementDate()).isEqualTo(MOVEMENT_DATE);
    assertThat(balances.settledAmountByInstallmentId(fixture.installmentIds().getFirst()))
        .isEqualByComparingTo("40.00");
    assertThat(version(fixture.accountId())).isEqualTo(1L);
    assertThat(status(fixture.accountId())).isEqualTo("APPROVED");
    assertThat(
            movements.findByScopeAndId(
                fixture.companyId(),
                fixture.accountId(),
                fixture.installmentIds().getFirst(),
                result.id()))
        .isPresent();
    assertThat(
            movements.findByScopeAndId(
                fixture.companyId() + 999,
                fixture.accountId(),
                fixture.installmentIds().getFirst(),
                result.id()))
        .isEmpty();
    assertThat(
            movements.findByScopeAndId(
                fixture.companyId(),
                fixture.accountId() + 999,
                fixture.installmentIds().getFirst(),
                result.id()))
        .isEmpty();
    assertThat(
            movements.findByScopeAndId(
                fixture.companyId(),
                fixture.accountId(),
                fixture.installmentIds().getFirst() + 999,
                result.id()))
        .isEmpty();
    assertThat(
            jdbc.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version='10'", Boolean.class))
        .isTrue();
  }

  @Test
  void receiptAndEveryPartialSettlementIncrementTheSameAccountVersion() {
    Fixture fixture = fixture("RECEIVABLE", List.of(new BigDecimal("100.00")));

    assertThat(settle.execute(command(fixture, 0, "20.00", "key-1")).type())
        .isEqualTo(FinancialMovementType.RECEIPT);
    assertThat(settle.execute(command(fixture, 0, "30.00", "key-2")).type())
        .isEqualTo(FinancialMovementType.RECEIPT);

    assertThat(version(fixture.accountId())).isEqualTo(2L);
    assertThat(status(fixture.accountId())).isEqualTo("APPROVED");
    assertThat(balances.settledAmountByInstallmentId(fixture.installmentIds().getFirst()))
        .isEqualByComparingTo("50.00");
  }

  @Test
  void accountBecomesSettledAtomicallyOnlyAfterEveryInstallmentIsClosed() {
    Fixture fixture =
        fixture("PAYABLE", List.of(new BigDecimal("100.00"), new BigDecimal("50.00")));

    settle.execute(command(fixture, 0, "100.00", "key-1"));
    assertThat(status(fixture.accountId())).isEqualTo("APPROVED");
    assertThat(balances.hasOpenInstallments(fixture.accountId())).isTrue();

    settle.execute(command(fixture, 1, "50.00", "key-2"));
    assertThat(status(fixture.accountId())).isEqualTo("SETTLED");
    assertThat(balances.hasOpenInstallments(fixture.accountId())).isFalse();
  }

  @Test
  void completedRetryReturnsSameMovementAndCreatesOneFinancialEffect() {
    Fixture fixture = fixture("PAYABLE", List.of(new BigDecimal("100.00")));
    var command = command(fixture, 0, "40.00", "same-key");

    SettlementResult first = settle.execute(command);
    SettlementResult replay = settle.execute(command);

    assertThat(replay).isEqualTo(first);
    assertThat(count("financialMovement")).isEqualTo(1);
    assertThat(count("idempotencyRecord")).isEqualTo(1);
  }

  @Test
  void failureAfterClaimAndVersionIncrementRollsBackEveryTechnicalAndFinancialEffect() {
    Fixture fixture = fixture("PAYABLE", List.of(new BigDecimal("100.00")));

    assertThatThrownBy(() -> settle.execute(command(fixture, 0, "101.00", "rollback-key")))
        .isInstanceOf(SettlementAmountExceedsBalanceException.class);

    assertThat(version(fixture.accountId())).isZero();
    assertThat(count("financialMovement")).isZero();
    assertThat(count("idempotencyRecord")).isZero();
    assertThat(status(fixture.accountId())).isEqualTo("APPROVED");
  }

  @Test
  void financialMovementInsertFailureRollsBackVersionAndIdempotencyClaim() {
    Fixture fixture = fixture("PAYABLE", List.of(new BigDecimal("100.00")));
    installFinancialMovementInsertFailure();

    assertThatThrownBy(() -> settle.execute(command(fixture, 0, "40.00", "insert-failure-key")))
        .isInstanceOf(RuntimeException.class);

    assertThat(version(fixture.accountId())).isZero();
    assertThat(status(fixture.accountId())).isEqualTo("APPROVED");
    assertThat(count("financialMovement")).isZero();
    assertThat(count("idempotencyRecord")).isZero();
    assertThat(balances.settledAmountByInstallmentId(fixture.installmentIds().getFirst()))
        .isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void finalStatusFailureAfterMovementInsertRollsBackCompleteSettlement() {
    Fixture fixture = fixture("PAYABLE", List.of(new BigDecimal("100.00")));
    installFinancialAccountSettlementUpdateFailure();

    assertThatThrownBy(() -> settle.execute(command(fixture, 0, "100.00", "status-failure-key")))
        .isInstanceOf(RuntimeException.class);

    assertThat(observedMovements.successfulSaves()).isEqualTo(1);
    assertThat(version(fixture.accountId())).isZero();
    assertThat(status(fixture.accountId())).isEqualTo("APPROVED");
    assertThat(count("financialMovement")).isZero();
    assertThat(count("idempotencyRecord")).isZero();
    assertThat(balances.settledAmountByInstallmentId(fixture.installmentIds().getFirst()))
        .isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void databaseChecksRejectInvalidTypeAndAmountAndForeignKeysProtectReferences() {
    Fixture fixture = fixture("PAYABLE", List.of(new BigDecimal("100.00")));
    Long installment = fixture.installmentIds().getFirst();
    String insert =
        "INSERT INTO \"financialMovement\"(\"installmentId\",\"type\",\"amount\",\"movementDate\",\"bankAccountId\",\"paymentMethodId\") VALUES(?,?,?,CURRENT_DATE,?,?)";

    assertThatThrownBy(
            () ->
                jdbc.update(
                    insert,
                    installment,
                    "INVALID",
                    BigDecimal.ONE,
                    fixture.bankAccountId(),
                    fixture.paymentMethodId()))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    insert,
                    installment,
                    "PAYMENT",
                    BigDecimal.ONE,
                    999999L,
                    fixture.paymentMethodId()))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    insert,
                    installment,
                    "PAYMENT",
                    BigDecimal.ONE,
                    fixture.bankAccountId(),
                    999999L))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    insert,
                    installment,
                    "PAYMENT",
                    BigDecimal.ZERO,
                    fixture.bankAccountId(),
                    fixture.paymentMethodId()))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    insert,
                    999999L,
                    "PAYMENT",
                    BigDecimal.ONE,
                    fixture.bankAccountId(),
                    fixture.paymentMethodId()))
        .isInstanceOf(DataIntegrityViolationException.class);

    settle.execute(command(fixture, 0, "10.00", "fk-key"));
    for (String delete :
        List.of(
            "DELETE FROM \"installment\" WHERE \"id\"=" + installment,
            "DELETE FROM \"bankAccount\" WHERE \"id\"=" + fixture.bankAccountId(),
            "DELETE FROM \"paymentMethod\" WHERE \"id\"=" + fixture.paymentMethodId())) {
      assertThatThrownBy(() -> jdbc.update(delete))
          .isInstanceOf(DataIntegrityViolationException.class);
    }
  }

  @Test
  void concurrentSixtyPlusSixtyNeverOverpaysAndRollsBackTheLoserClaim() throws Exception {
    Fixture fixture = fixture("PAYABLE", List.of(new BigDecimal("100.00")));
    coordinatedAccounts.arm(2);

    List<Outcome> outcomes =
        concurrent(command(fixture, 0, "60.00", "key-a"), command(fixture, 0, "60.00", "key-b"));

    assertThat(outcomes).filteredOn(Outcome::success).hasSize(1);
    assertThat(outcomes).filteredOn(outcome -> !outcome.success()).hasSize(1);
    assertThat(
            outcomes.stream()
                .filter(outcome -> !outcome.success())
                .findFirst()
                .orElseThrow()
                .error())
        .isInstanceOf(SettlementConflictException.class);
    assertThat(balances.settledAmountByInstallmentId(fixture.installmentIds().getFirst()))
        .isEqualByComparingTo("60.00");
    assertThat(count("financialMovement")).isEqualTo(1);
    assertThat(count("idempotencyRecord")).isEqualTo(1);
    assertThat(status(fixture.accountId())).isEqualTo("APPROVED");
  }

  @Test
  void concurrentSixtyPlusFortyCanRetryTheRolledBackLoserAndFinishExactlyAtZero() throws Exception {
    Fixture fixture = fixture("PAYABLE", List.of(new BigDecimal("100.00")));
    var sixty = command(fixture, 0, "60.00", "key-60");
    var forty = command(fixture, 0, "40.00", "key-40");
    coordinatedAccounts.arm(2);

    List<Outcome> outcomes = concurrent(sixty, forty);
    assertThat(outcomes).filteredOn(Outcome::success).hasSize(1);
    List<Outcome> failures = outcomes.stream().filter(outcome -> !outcome.success()).toList();
    assertThat(failures).hasSize(1);
    assertThat(failures.getFirst().error()).isInstanceOf(SettlementConflictException.class);
    SettleInstallmentCommand retry = failures.getFirst().command();
    coordinatedAccounts.disarm();
    settle.execute(retry);

    assertThat(balances.settledAmountByInstallmentId(fixture.installmentIds().getFirst()))
        .isEqualByComparingTo("100.00");
    assertThat(status(fixture.accountId())).isEqualTo("SETTLED");
    assertThat(count("financialMovement")).isEqualTo(2);
    assertThat(count("idempotencyRecord")).isEqualTo(2);
  }

  @Test
  void concurrentSameKeyAndFingerprintCreateAtMostOneMovement() throws Exception {
    Fixture fixture = fixture("PAYABLE", List.of(new BigDecimal("100.00")));
    var command = command(fixture, 0, "40.00", "shared-key");

    List<Outcome> outcomes = concurrent(command, command);

    assertThat(outcomes).allMatch(Outcome::success);
    Long movementId = outcomes.getFirst().result().id();
    assertThat(outcomes).extracting(outcome -> outcome.result().id()).containsOnly(movementId);
    assertThat(count("financialMovement")).isEqualTo(1);
    assertThat(count("idempotencyRecord")).isEqualTo(1);
  }

  private List<Outcome> concurrent(SettleInstallmentCommand first, SettleInstallmentCommand second)
      throws Exception {
    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      CyclicBarrier start = new CyclicBarrier(2);
      Future<Outcome> one = executor.submit(() -> executeAfter(start, first));
      Future<Outcome> two = executor.submit(() -> executeAfter(start, second));
      return List.of(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS));
    }
  }

  private Outcome executeAfter(CyclicBarrier start, SettleInstallmentCommand command) {
    CoordinatedFinancialAccountRepository.await(start);
    return execute(command);
  }

  private Outcome execute(SettleInstallmentCommand command) {
    try {
      return new Outcome(command, settle.execute(command), null);
    } catch (RuntimeException exception) {
      return new Outcome(command, null, exception);
    }
  }

  private Fixture fixture(String type, List<BigDecimal> installmentAmounts) {
    Long company =
        jdbc.queryForObject(
            "INSERT INTO \"company\"(\"name\") VALUES(?) RETURNING \"id\"",
            Long.class,
            "Settlement Company");
    Long branch =
        jdbc.queryForObject(
            "INSERT INTO \"branch\"(\"companyId\",\"name\") VALUES(?,?) RETURNING \"id\"",
            Long.class,
            company,
            "Settlement Branch");
    Long partner =
        jdbc.queryForObject(
            "INSERT INTO \"partner\"(\"name\",\"document\",\"customer\",\"supplier\",\"active\") VALUES('Partner','52998224725',TRUE,TRUE,TRUE) RETURNING \"id\"",
            Long.class);
    Long category =
        jdbc.queryForObject(
            "INSERT INTO \"category\"(\"companyId\",\"name\",\"active\") VALUES(?,'Category',TRUE) RETURNING \"id\"",
            Long.class,
            company);
    Long bank =
        jdbc.queryForObject(
            "INSERT INTO \"bankAccount\"(\"companyId\",\"branchId\",\"name\",\"active\") VALUES(?,NULL,'Bank',TRUE) RETURNING \"id\"",
            Long.class,
            company);
    Long payment =
        jdbc.queryForObject(
            "INSERT INTO \"paymentMethod\"(\"companyId\",\"name\",\"active\") VALUES(?,'PIX',TRUE) RETURNING \"id\"",
            Long.class,
            company);
    BigDecimal total = installmentAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    Long account =
        jdbc.queryForObject(
            "INSERT INTO \"financialAccount\"(\"companyId\",\"branchId\",\"type\",\"partnerId\",\"categoryId\",\"issueDate\",\"totalAmount\",\"status\") VALUES(?,?,?,?,?,CURRENT_DATE,?,'APPROVED') RETURNING \"id\"",
            Long.class,
            company,
            branch,
            type,
            partner,
            category,
            total);
    var installments = new java.util.ArrayList<Long>();
    for (int index = 0; index < installmentAmounts.size(); index++) {
      installments.add(
          jdbc.queryForObject(
              "INSERT INTO \"installment\"(\"financialAccountId\",\"installmentNumber\",\"dueDate\",\"amount\") VALUES(?,?,CURRENT_DATE,?) RETURNING \"id\"",
              Long.class,
              account,
              index + 1,
              installmentAmounts.get(index)));
    }
    return new Fixture(company, account, List.copyOf(installments), bank, payment);
  }

  private SettleInstallmentCommand command(
      Fixture fixture, int installmentIndex, String amount, String key) {
    return new SettleInstallmentCommand(
        fixture.companyId(),
        fixture.accountId(),
        fixture.installmentIds().get(installmentIndex),
        new BigDecimal(amount),
        MOVEMENT_DATE,
        fixture.bankAccountId(),
        fixture.paymentMethodId(),
        key);
  }

  private void installFinancialMovementInsertFailure() {
    jdbc.execute(
        "CREATE FUNCTION \"failFinancialMovementInsertFunction\"() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'forced financial movement insert failure'; END $$");
    jdbc.execute(
        "CREATE TRIGGER \"failFinancialMovementInsert\" BEFORE INSERT ON \"financialMovement\" FOR EACH ROW EXECUTE FUNCTION \"failFinancialMovementInsertFunction\"() ");
  }

  private void installFinancialAccountSettlementUpdateFailure() {
    jdbc.execute(
        "CREATE FUNCTION \"failFinancialAccountSettlementUpdateFunction\"() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'forced financial account settlement update failure'; END $$");
    jdbc.execute(
        "CREATE TRIGGER \"failFinancialAccountSettlementUpdate\" BEFORE UPDATE OF \"status\" ON \"financialAccount\" FOR EACH ROW EXECUTE FUNCTION \"failFinancialAccountSettlementUpdateFunction\"() ");
  }

  private long count(String table) {
    return jdbc.queryForObject("SELECT COUNT(*) FROM \"" + table + "\"", Long.class);
  }

  private long version(Long accountId) {
    return jdbc.queryForObject(
        "SELECT \"version\" FROM \"financialAccount\" WHERE \"id\"=?", Long.class, accountId);
  }

  private String status(Long accountId) {
    return jdbc.queryForObject(
        "SELECT \"status\" FROM \"financialAccount\" WHERE \"id\"=?", String.class, accountId);
  }

  private record Fixture(
      Long companyId,
      Long accountId,
      List<Long> installmentIds,
      Long bankAccountId,
      Long paymentMethodId) {}

  private record Outcome(
      SettleInstallmentCommand command, SettlementResult result, RuntimeException error) {
    boolean success() {
      return result != null;
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class ConcurrencyConfiguration {
    @Bean
    @Primary
    CoordinatedFinancialAccountRepository coordinatedFinancialAccountRepository(
        @Qualifier("jpaFinancialAccountRepositoryAdapter") FinancialAccountRepository delegate) {
      return new CoordinatedFinancialAccountRepository(delegate);
    }

    @Bean
    @Primary
    ObservedFinancialMovementRepository observedFinancialMovementRepository(
        @Qualifier("jpaFinancialMovementRepositoryAdapter") FinancialMovementRepository delegate) {
      return new ObservedFinancialMovementRepository(delegate);
    }
  }

  static final class ObservedFinancialMovementRepository implements FinancialMovementRepository {
    private final FinancialMovementRepository delegate;
    private final java.util.concurrent.atomic.AtomicInteger successfulSaves =
        new java.util.concurrent.atomic.AtomicInteger();

    ObservedFinancialMovementRepository(FinancialMovementRepository delegate) {
      this.delegate = delegate;
    }

    @Override
    public com.financeiro.financialmovement.domain.FinancialMovement save(
        com.financeiro.financialmovement.domain.FinancialMovement movement) {
      var saved = delegate.save(movement);
      successfulSaves.incrementAndGet();
      return saved;
    }

    @Override
    public Optional<com.financeiro.financialmovement.domain.FinancialMovement> findByScopeAndId(
        Long companyId, Long financialAccountId, Long installmentId, Long movementId) {
      return delegate.findByScopeAndId(companyId, financialAccountId, installmentId, movementId);
    }

    int successfulSaves() {
      return successfulSaves.get();
    }

    void reset() {
      successfulSaves.set(0);
    }
  }

  static final class CoordinatedFinancialAccountRepository implements FinancialAccountRepository {
    private final FinancialAccountRepository delegate;
    private volatile CyclicBarrier barrier;

    CoordinatedFinancialAccountRepository(FinancialAccountRepository delegate) {
      this.delegate = delegate;
    }

    void arm(int parties) {
      barrier = new CyclicBarrier(parties);
    }

    void disarm() {
      barrier = null;
    }

    @Override
    public FinancialAccount save(FinancialAccount account) {
      return delegate.save(account);
    }

    @Override
    public FinancialAccount updateStatus(FinancialAccount account) {
      return delegate.updateStatus(account);
    }

    @Override
    public void forceSettlementVersionIncrement(Long companyId, Long financialAccountId) {
      CyclicBarrier current = barrier;
      if (current != null) await(current);
      delegate.forceSettlementVersionIncrement(companyId, financialAccountId);
    }

    @Override
    public FinancialAccount updateSettlementStatus(FinancialAccount account) {
      return delegate.updateSettlementStatus(account);
    }

    @Override
    public Optional<FinancialAccount> findByCompanyIdAndId(
        Long companyId, Long financialAccountId) {
      return delegate.findByCompanyIdAndId(companyId, financialAccountId);
    }

    @Override
    public PageResult<FinancialAccountSummary> findSummaryPageByCompanyId(
        Long companyId, FinancialAccountPageQuery query) {
      return delegate.findSummaryPageByCompanyId(companyId, query);
    }

    static void await(CyclicBarrier barrier) {
      try {
        barrier.await(10, TimeUnit.SECONDS);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(
            "Settlement concurrency coordination interrupted", exception);
      } catch (BrokenBarrierException | TimeoutException exception) {
        throw new IllegalStateException("Settlement concurrency coordination failed", exception);
      }
    }
  }
}
