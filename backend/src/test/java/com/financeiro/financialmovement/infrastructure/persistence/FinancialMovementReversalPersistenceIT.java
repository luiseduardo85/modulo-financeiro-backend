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
  FinancialMovementReversalPersistenceIT.ConcurrencyConfiguration.class
})
class FinancialMovementReversalPersistenceIT {
  private static final LocalDate MOVEMENT_DATE = LocalDate.of(2026, 8, 22);
  @Autowired JdbcTemplate jdbc;
  @Autowired SettleInstallment settle;
  @Autowired ReverseFinancialMovement reverse;
  @Autowired FinancialMovementRepository movements;
  @Autowired SettlementBalanceRepository balances;
  @Autowired CoordinatedFinancialAccountRepository coordinatedAccounts;

  @BeforeEach
  @AfterEach
  void clean() {
    coordinatedAccounts.disarm();
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
  void v11MappingPersistsReversalReferencingOriginalWithNetBalanceAndScopedReplay() {
    Fixture fixture = fixture("PAYABLE", List.of(new BigDecimal("100.00")));
    Long installmentId = fixture.installmentIds().getFirst();
    SettlementResult original = settle.execute(settleCommand(fixture, 0, "100.00", "settle-key"));

    ReversalResult result =
        reverse.execute(reversalCommand(fixture, original.id(), "40.00", "rev-key"));

    assertThat(result.type()).isEqualTo(FinancialMovementType.REVERSAL_PAYMENT);
    assertThat(result.originalMovementId()).isEqualTo(original.id());
    assertThat(result.amount()).isEqualByComparingTo("40.00");
    assertThat(balances.settledAmountByInstallmentId(installmentId)).isEqualByComparingTo("60.00");
    assertThat(balances.reversedAmountByOriginalMovementId(original.id()))
        .isEqualByComparingTo("40.00");
    assertThat(status(fixture.accountId())).isEqualTo("APPROVED");
    assertThat(
            movements.findByScopeAndId(
                fixture.companyId(), fixture.accountId(), installmentId, result.id()))
        .isPresent();
    assertThat(
            movements.findByScopeAndId(
                fixture.companyId() + 999, fixture.accountId(), installmentId, result.id()))
        .isEmpty();
    assertThat(
            movements.findByScopeAndId(
                fixture.companyId(), fixture.accountId() + 999, installmentId, result.id()))
        .isEmpty();
    assertThat(
            movements.findByScopeAndId(
                fixture.companyId(), fixture.accountId(), installmentId + 999, result.id()))
        .isEmpty();
    assertThat(
            jdbc.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version='11'", Boolean.class))
        .isTrue();
  }

  @Test
  void fullReversalReopensSettledAccountToApproved() {
    Fixture fixture = fixture("PAYABLE", List.of(new BigDecimal("100.00")));
    SettlementResult original = settle.execute(settleCommand(fixture, 0, "100.00", "settle-key"));
    assertThat(status(fixture.accountId())).isEqualTo("SETTLED");
    long versionAfterSettlement = version(fixture.accountId());

    reverse.execute(reversalCommand(fixture, original.id(), "40.00", "rev-key"));

    assertThat(status(fixture.accountId())).isEqualTo("APPROVED");
    assertThat(version(fixture.accountId())).isGreaterThan(versionAfterSettlement);
  }

  @Test
  void reversalWhileAccountStillApprovedDoesNotChangeStatus() {
    Fixture fixture = fixture("PAYABLE", List.of(new BigDecimal("100.00")));
    SettlementResult original = settle.execute(settleCommand(fixture, 0, "60.00", "settle-key"));
    assertThat(status(fixture.accountId())).isEqualTo("APPROVED");

    reverse.execute(reversalCommand(fixture, original.id(), "20.00", "rev-key"));

    assertThat(status(fixture.accountId())).isEqualTo("APPROVED");
    assertThat(balances.settledAmountByInstallmentId(fixture.installmentIds().getFirst()))
        .isEqualByComparingTo("40.00");
  }

  @Test
  void completedRetryReturnsSameReversalAndCreatesOneFinancialEffect() {
    Fixture fixture = fixture("PAYABLE", List.of(new BigDecimal("100.00")));
    SettlementResult original = settle.execute(settleCommand(fixture, 0, "100.00", "settle-key"));
    var command = reversalCommand(fixture, original.id(), "40.00", "same-key");

    ReversalResult first = reverse.execute(command);
    ReversalResult replay = reverse.execute(command);

    assertThat(replay).isEqualTo(first);
    assertThat(count("financialMovement")).isEqualTo(2);
    assertThat(count("idempotencyRecord")).isEqualTo(2);
  }

  @Test
  void overReversalRollsBackEveryTechnicalAndFinancialEffect() {
    Fixture fixture = fixture("PAYABLE", List.of(new BigDecimal("100.00")));
    SettlementResult original = settle.execute(settleCommand(fixture, 0, "100.00", "settle-key"));
    long versionAfterSettlement = version(fixture.accountId());

    assertThatThrownBy(
            () ->
                reverse.execute(reversalCommand(fixture, original.id(), "101.00", "rollback-key")))
        .isInstanceOf(ReversalAmountExceedsBalanceException.class);

    assertThat(version(fixture.accountId())).isEqualTo(versionAfterSettlement);
    assertThat(count("financialMovement")).isEqualTo(1);
    assertThat(count("idempotencyRecord")).isEqualTo(1);
    assertThat(status(fixture.accountId())).isEqualTo("SETTLED");
  }

  @Test
  void reversalInsertFailureRollsBackVersionAndIdempotencyClaim() {
    Fixture fixture = fixture("PAYABLE", List.of(new BigDecimal("100.00")));
    SettlementResult original = settle.execute(settleCommand(fixture, 0, "100.00", "settle-key"));
    long versionAfterSettlement = version(fixture.accountId());
    installFinancialMovementInsertFailure();

    assertThatThrownBy(
            () ->
                reverse.execute(
                    reversalCommand(fixture, original.id(), "40.00", "insert-failure-key")))
        .isInstanceOf(RuntimeException.class);

    assertThat(version(fixture.accountId())).isEqualTo(versionAfterSettlement);
    assertThat(status(fixture.accountId())).isEqualTo("SETTLED");
    assertThat(count("financialMovement")).isEqualTo(1);
    assertThat(count("idempotencyRecord")).isEqualTo(1);
    assertThat(balances.reversedAmountByOriginalMovementId(original.id()))
        .isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void finalStatusFailureAfterReversalInsertRollsBackCompleteReopen() {
    Fixture fixture = fixture("PAYABLE", List.of(new BigDecimal("100.00")));
    SettlementResult original = settle.execute(settleCommand(fixture, 0, "100.00", "settle-key"));
    long versionAfterSettlement = version(fixture.accountId());
    installFinancialAccountSettlementUpdateFailure();

    assertThatThrownBy(
            () ->
                reverse.execute(
                    reversalCommand(fixture, original.id(), "40.00", "status-failure-key")))
        .isInstanceOf(RuntimeException.class);

    assertThat(version(fixture.accountId())).isEqualTo(versionAfterSettlement);
    assertThat(status(fixture.accountId())).isEqualTo("SETTLED");
    assertThat(count("financialMovement")).isEqualTo(1);
    assertThat(count("idempotencyRecord")).isEqualTo(1);
    assertThat(balances.reversedAmountByOriginalMovementId(original.id()))
        .isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void databaseChecksRejectInvalidReversalRowsAndForeignKeysProtectReferences() {
    Fixture fixture = fixture("PAYABLE", List.of(new BigDecimal("100.00")));
    SettlementResult original = settle.execute(settleCommand(fixture, 0, "100.00", "settle-key"));
    Long installment = fixture.installmentIds().getFirst();
    String insert =
        "INSERT INTO \"financialMovement\"(\"installmentId\",\"type\",\"amount\",\"movementDate\",\"bankAccountId\",\"paymentMethodId\",\"originalMovementId\") VALUES(?,?,?,CURRENT_DATE,?,?,?)";

    assertThatThrownBy(
            () ->
                jdbc.update(
                    insert,
                    installment,
                    "REVERSAL_PAYMENT",
                    BigDecimal.ONE,
                    fixture.bankAccountId(),
                    fixture.paymentMethodId(),
                    999999L))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    insert,
                    installment,
                    "PAYMENT",
                    BigDecimal.ONE,
                    fixture.bankAccountId(),
                    fixture.paymentMethodId(),
                    original.id()))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    insert,
                    installment,
                    "REVERSAL_PAYMENT",
                    BigDecimal.ONE,
                    fixture.bankAccountId(),
                    fixture.paymentMethodId(),
                    null))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    insert,
                    installment,
                    "BOGUS_TYPE",
                    BigDecimal.ONE,
                    fixture.bankAccountId(),
                    fixture.paymentMethodId(),
                    original.id()))
        .isInstanceOf(DataIntegrityViolationException.class);

    reverse.execute(reversalCommand(fixture, original.id(), "10.00", "fk-key"));
    assertThatThrownBy(
            () -> jdbc.update("DELETE FROM \"financialMovement\" WHERE \"id\"=?", original.id()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void concurrentSixtyPlusSixtyReversalsNeverExceedTheOriginalAmount() throws Exception {
    Fixture fixture = fixture("PAYABLE", List.of(new BigDecimal("100.00")));
    SettlementResult original = settle.execute(settleCommand(fixture, 0, "100.00", "settle-key"));
    coordinatedAccounts.arm(2);

    List<Outcome> outcomes =
        concurrentReversals(
            reversalCommand(fixture, original.id(), "60.00", "rev-a"),
            reversalCommand(fixture, original.id(), "60.00", "rev-b"));

    assertThat(outcomes).filteredOn(Outcome::success).hasSize(1);
    assertThat(outcomes).filteredOn(outcome -> !outcome.success()).hasSize(1);
    assertThat(
            outcomes.stream()
                .filter(outcome -> !outcome.success())
                .findFirst()
                .orElseThrow()
                .error())
        .isInstanceOf(SettlementConflictException.class);
    assertThat(balances.reversedAmountByOriginalMovementId(original.id()))
        .isEqualByComparingTo("60.00");
    assertThat(count("financialMovement")).isEqualTo(2);
    assertThat(count("idempotencyRecord")).isEqualTo(2);
  }

  @Test
  void concurrentSixtyPlusFortyReversalsCanRetryTheLoserAndFullyReverseTheOriginal()
      throws Exception {
    Fixture fixture = fixture("PAYABLE", List.of(new BigDecimal("100.00")));
    SettlementResult original = settle.execute(settleCommand(fixture, 0, "100.00", "settle-key"));
    var sixty = reversalCommand(fixture, original.id(), "60.00", "rev-60");
    var forty = reversalCommand(fixture, original.id(), "40.00", "rev-40");
    coordinatedAccounts.arm(2);

    List<Outcome> outcomes = concurrentReversals(sixty, forty);
    assertThat(outcomes).filteredOn(Outcome::success).hasSize(1);
    List<Outcome> failures = outcomes.stream().filter(outcome -> !outcome.success()).toList();
    assertThat(failures).hasSize(1);
    assertThat(failures.getFirst().error()).isInstanceOf(SettlementConflictException.class);
    ReverseFinancialMovementCommand retry = failures.getFirst().command();
    coordinatedAccounts.disarm();
    reverse.execute(retry);

    assertThat(balances.reversedAmountByOriginalMovementId(original.id()))
        .isEqualByComparingTo("100.00");
    assertThat(status(fixture.accountId())).isEqualTo("APPROVED");
    assertThat(count("financialMovement")).isEqualTo(3);
    assertThat(count("idempotencyRecord")).isEqualTo(3);
  }

  @Test
  void concurrentSameKeyAndFingerprintReversalsCreateAtMostOneReversal() throws Exception {
    Fixture fixture = fixture("PAYABLE", List.of(new BigDecimal("100.00")));
    SettlementResult original = settle.execute(settleCommand(fixture, 0, "100.00", "settle-key"));
    var command = reversalCommand(fixture, original.id(), "40.00", "shared-key");

    List<Outcome> outcomes = concurrentReversals(command, command);

    assertThat(outcomes).allMatch(Outcome::success);
    Long reversalId = outcomes.getFirst().result().id();
    assertThat(outcomes).extracting(outcome -> outcome.result().id()).containsOnly(reversalId);
    assertThat(count("financialMovement")).isEqualTo(2);
    assertThat(count("idempotencyRecord")).isEqualTo(2);
  }

  private List<Outcome> concurrentReversals(
      ReverseFinancialMovementCommand first, ReverseFinancialMovementCommand second)
      throws Exception {
    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      CyclicBarrier start = new CyclicBarrier(2);
      Future<Outcome> one = executor.submit(() -> executeAfter(start, first));
      Future<Outcome> two = executor.submit(() -> executeAfter(start, second));
      return List.of(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS));
    }
  }

  private Outcome executeAfter(CyclicBarrier start, ReverseFinancialMovementCommand command) {
    FinancialMovementPersistenceIT.CoordinatedFinancialAccountRepository.await(start);
    return execute(command);
  }

  private Outcome execute(ReverseFinancialMovementCommand command) {
    try {
      return new Outcome(command, reverse.execute(command), null);
    } catch (RuntimeException exception) {
      return new Outcome(command, null, exception);
    }
  }

  private Fixture fixture(String type, List<BigDecimal> installmentAmounts) {
    Long company =
        jdbc.queryForObject(
            "INSERT INTO \"company\"(\"name\") VALUES(?) RETURNING \"id\"",
            Long.class,
            "Reversal Company");
    Long branch =
        jdbc.queryForObject(
            "INSERT INTO \"branch\"(\"companyId\",\"name\") VALUES(?,?) RETURNING \"id\"",
            Long.class,
            company,
            "Reversal Branch");
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

  private SettleInstallmentCommand settleCommand(
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

  private ReverseFinancialMovementCommand reversalCommand(
      Fixture fixture, Long originalMovementId, String amount, String key) {
    return new ReverseFinancialMovementCommand(
        fixture.companyId(),
        fixture.accountId(),
        fixture.installmentIds().getFirst(),
        originalMovementId,
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
      ReverseFinancialMovementCommand command, ReversalResult result, RuntimeException error) {
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
      if (current != null)
        FinancialMovementPersistenceIT.CoordinatedFinancialAccountRepository.await(current);
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
  }
}
