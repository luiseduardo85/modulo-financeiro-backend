package com.financeiro.idempotency.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financeiro.idempotency.application.FinancialIdempotencyService;
import com.financeiro.idempotency.application.IdempotencyClaim;
import com.financeiro.idempotency.application.IdempotencyConflictException;
import com.financeiro.idempotency.application.IdempotencyScope;
import com.financeiro.integration.support.PostgresIntegrationTestConfiguration;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("it")
@Import(PostgresIntegrationTestConfiguration.class)
class PostgresIdempotencyStoreIT {

  private static final String FINGERPRINT = "a".repeat(64);
  private static final long DATABASE_WAIT_TIMEOUT_SECONDS = 10;

  @Autowired private FinancialIdempotencyService service;

  @Autowired private TransactionTemplate transactions;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @BeforeEach
  void deleteRecords() {
    jdbcTemplate.update("DELETE FROM \"idempotencyRecord\"");
  }

  @Test
  void mappingMatchesQuotedCamelCaseSchemaAndFirstClaimSucceeds() {
    IdempotencyClaim claim =
        transactions.execute(
            status -> {
              IdempotencyClaim created =
                  service.claim(scope(1L, "OPERATION_A", "key-1"), FINGERPRINT);
              status.setRollbackOnly();
              return created;
            });

    String physicalTable =
        jdbcTemplate.queryForObject(
            "SELECT to_regclass('\"idempotencyRecord\"')::text", String.class);
    Set<String> entityNames =
        entityManagerFactory.getMetamodel().getEntities().stream()
            .map(entity -> entity.getName())
            .collect(java.util.stream.Collectors.toSet());

    assertThat(claim.outcome()).isEqualTo(IdempotencyClaim.Outcome.CLAIMED);
    assertThat(physicalTable).isEqualTo("\"idempotencyRecord\"");
    assertThat(entityNames).contains("IdempotencyRecordEntity");
  }

  @Test
  void compatibleCompletedRetryReturnsStoredReference() {
    completeFirstExecution(scope(1L, "OPERATION_A", "key-1"), "result-1");

    IdempotencyClaim retry =
        transactions.execute(
            status -> service.claim(scope(1L, "OPERATION_A", "key-1"), FINGERPRINT));

    assertThat(retry.outcome()).isEqualTo(IdempotencyClaim.Outcome.COMPLETED);
    assertThat(retry.resultReference()).isEqualTo("result-1");
  }

  @Test
  void incompatibleFingerprintConflicts() {
    IdempotencyScope scope = scope(1L, "OPERATION_A", "key-1");
    completeFirstExecution(scope, "result-1");

    assertThatThrownBy(() -> transactions.execute(status -> service.claim(scope, "b".repeat(64))))
        .isInstanceOf(IdempotencyConflictException.class);
  }

  @Test
  void companyAndOperationAreIndependentScopes() {
    List<IdempotencyClaim> claims =
        transactions.execute(
            status -> {
              List<IdempotencyClaim> created =
                  List.of(
                      service.claim(scope(1L, "OPERATION_A", "shared-key"), FINGERPRINT),
                      service.claim(scope(2L, "OPERATION_A", "shared-key"), FINGERPRINT),
                      service.claim(scope(1L, "OPERATION_B", "shared-key"), FINGERPRINT));
              service.complete(created.get(0), "company-1-operation-a");
              service.complete(created.get(1), "company-2-operation-a");
              service.complete(created.get(2), "company-1-operation-b");
              return created;
            });

    assertThat(claims).allMatch(claim -> claim.outcome() == IdempotencyClaim.Outcome.CLAIMED);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"idempotencyRecord\"", Integer.class))
        .isEqualTo(3);
  }

  @Test
  void completionPersistsOnlyTechnicalResultState() {
    completeFirstExecution(scope(1L, "OPERATION_A", "key-1"), "result-1");

    var row =
        jdbcTemplate.queryForMap(
            """
                SELECT "status", "resultReference", "completedAt"
                FROM "idempotencyRecord"
                WHERE "companyId" = 1 AND "operation" = 'OPERATION_A' AND "idempotencyKey" = 'key-1'
                """);

    assertThat(row.get("status")).isEqualTo("COMPLETED");
    assertThat(row.get("resultReference")).isEqualTo("result-1");
    assertThat(row.get("completedAt")).isNotNull();
  }

  @Test
  void concurrentDuplicatesYieldOneClaimAndOneCompletedRetry() throws Exception {
    IdempotencyScope scope = scope(1L, "OPERATION_A", "concurrent-key");
    CountDownLatch winnerReady = new CountDownLatch(1);
    CountDownLatch allowWinnerCommit = new CountDownLatch(1);
    CompletableFuture<Integer> winnerBackendPid = new CompletableFuture<>();
    CompletableFuture<Integer> loserBackendPid = new CompletableFuture<>();

    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      Future<IdempotencyClaim> winner =
          executor.submit(
              () ->
                  transactions.execute(
                      status -> {
                        winnerBackendPid.complete(currentBackendPid());
                        IdempotencyClaim claim = service.claim(scope, FINGERPRINT);
                        service.complete(claim, "result-1");
                        winnerReady.countDown();
                        await(allowWinnerCommit);
                        return claim;
                      }));

      assertThat(winnerReady.await(10, TimeUnit.SECONDS)).isTrue();
      Future<IdempotencyClaim> loser =
          executor.submit(
              () -> {
                return transactions.execute(
                    status -> {
                      loserBackendPid.complete(currentBackendPid());
                      return service.claim(scope, FINGERPRINT);
                    });
              });
      awaitPostgresContention(
          winnerBackendPid.get(10, TimeUnit.SECONDS), loserBackendPid.get(10, TimeUnit.SECONDS));
      allowWinnerCommit.countDown();

      assertThat(winner.get(10, TimeUnit.SECONDS).outcome())
          .isEqualTo(IdempotencyClaim.Outcome.CLAIMED);
      IdempotencyClaim retry = loser.get(10, TimeUnit.SECONDS);
      assertThat(retry.outcome()).isEqualTo(IdempotencyClaim.Outcome.COMPLETED);
      assertThat(retry.resultReference()).isEqualTo("result-1");
    }

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"idempotencyRecord\"", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void waitingAttemptClaimsAfterWinnerRollsBack() throws Exception {
    IdempotencyScope scope = scope(1L, "OPERATION_A", "rollback-key");
    CountDownLatch winnerReady = new CountDownLatch(1);
    CountDownLatch rollbackWinner = new CountDownLatch(1);
    CompletableFuture<Integer> winnerBackendPid = new CompletableFuture<>();
    CompletableFuture<Integer> loserBackendPid = new CompletableFuture<>();

    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      Future<?> winner =
          executor.submit(
              () ->
                  assertThatThrownBy(
                          () ->
                              transactions.execute(
                                  status -> {
                                    winnerBackendPid.complete(currentBackendPid());
                                    service.claim(scope, FINGERPRINT);
                                    winnerReady.countDown();
                                    await(rollbackWinner);
                                    throw new IllegalStateException("rollback winner");
                                  }))
                      .isInstanceOf(IllegalStateException.class));

      assertThat(winnerReady.await(10, TimeUnit.SECONDS)).isTrue();
      Future<IdempotencyClaim> loser =
          executor.submit(
              () -> {
                return transactions.execute(
                    status -> {
                      loserBackendPid.complete(currentBackendPid());
                      IdempotencyClaim claim = service.claim(scope, FINGERPRINT);
                      service.complete(claim, "result-after-rollback");
                      return claim;
                    });
              });
      awaitPostgresContention(
          winnerBackendPid.get(10, TimeUnit.SECONDS), loserBackendPid.get(10, TimeUnit.SECONDS));
      rollbackWinner.countDown();

      winner.get(10, TimeUnit.SECONDS);
      assertThat(loser.get(10, TimeUnit.SECONDS).outcome())
          .isEqualTo(IdempotencyClaim.Outcome.CLAIMED);
    }

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT \"resultReference\" FROM \"idempotencyRecord\"", String.class))
        .isEqualTo("result-after-rollback");
  }

  @Test
  void databaseConstraintsRejectInvalidTechnicalState() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                INSERT INTO "idempotencyRecord" (
                    "companyId", "operation", "idempotencyKey", "fingerprint", "status", "createdAt"
                ) VALUES (1, 'OPERATION_A', 'key-1', ?, 'FAILED', CURRENT_TIMESTAMP)
                """,
                    FINGERPRINT))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private void completeFirstExecution(IdempotencyScope scope, String resultReference) {
    transactions.executeWithoutResult(
        status -> {
          IdempotencyClaim claim = service.claim(scope, FINGERPRINT);
          service.complete(claim, resultReference);
        });
  }

  private IdempotencyScope scope(long companyId, String operation, String key) {
    return new IdempotencyScope(companyId, operation, key);
  }

  private int currentBackendPid() {
    return jdbcTemplate.queryForObject("SELECT pg_backend_pid()", Integer.class);
  }

  private void awaitPostgresContention(int winnerBackendPid, int loserBackendPid) {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(DATABASE_WAIT_TIMEOUT_SECONDS);
    do {
      Boolean loserIsBlockedByWinner =
          jdbcTemplate.queryForObject(
              """
                    SELECT EXISTS (
                        SELECT 1
                        FROM pg_stat_activity
                        WHERE pid = ?
                          AND wait_event_type = 'Lock'
                          AND ? = ANY(pg_blocking_pids(pid))
                    )
                    """,
              Boolean.class,
              loserBackendPid,
              winnerBackendPid);
      if (Boolean.TRUE.equals(loserIsBlockedByWinner)) {
        return;
      }
      LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
    } while (System.nanoTime() < deadline);

    throw new AssertionError(
        "PostgreSQL contention was not observed within "
            + DATABASE_WAIT_TIMEOUT_SECONDS
            + " seconds: backend "
            + loserBackendPid
            + " was expected to wait for backend "
            + winnerBackendPid);
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("timed out waiting for test coordination");
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("test coordination interrupted", exception);
    }
  }
}
