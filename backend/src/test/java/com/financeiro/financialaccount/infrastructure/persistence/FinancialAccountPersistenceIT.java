package com.financeiro.financialaccount.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.financeiro.category.application.CategoryRepository;
import com.financeiro.category.domain.Category;
import com.financeiro.company.application.*;
import com.financeiro.company.domain.*;
import com.financeiro.costcenter.application.CostCenterRepository;
import com.financeiro.costcenter.domain.CostCenter;
import com.financeiro.financialaccount.application.*;
import com.financeiro.financialaccount.domain.*;
import com.financeiro.integration.support.PostgresIntegrationTestConfiguration;
import com.financeiro.partner.application.PartnerRepository;
import com.financeiro.partner.domain.*;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("it")
@Import(PostgresIntegrationTestConfiguration.class)
class FinancialAccountPersistenceIT {
  @Autowired CompanyRepository companies;
  @Autowired BranchRepository branches;
  @Autowired PartnerRepository partners;
  @Autowired CategoryRepository categories;
  @Autowired CostCenterRepository costCenters;
  @Autowired FinancialAccountRepository accounts;
  @Autowired SpringDataFinancialAccountRepository springDataAccounts;
  @Autowired JdbcTemplate jdbc;
  @Autowired EntityManagerFactory entityManagerFactory;
  @Autowired PlatformTransactionManager transactionManager;

  @BeforeEach
  @AfterEach
  void clean() {
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
  void v8PersistsAndReloadsCompleteAggregateWithIdentitiesMoneyAndDates() {
    var f = fixtures("One");
    var saved = accounts.save(account(f, FinancialAccountType.PAYABLE, null));
    assertThat(saved.id()).isPositive();
    assertThat(saved.installments())
        .extracting(Installment::id)
        .allMatch(id -> id != null && id > 0);
    assertThat(saved.installments())
        .extracting(Installment::installmentNumber)
        .containsExactly(1, 3);
    assertThat(saved.totalAmount()).isEqualByComparingTo("150.00");
    assertThat(saved.issueDate()).isEqualTo(LocalDate.of(2026, 8, 21));
    var reloaded = accounts.findByCompanyIdAndId(f.company.id(), saved.id()).orElseThrow();
    assertThat(reloaded.id()).isEqualTo(saved.id());
    assertThat(reloaded.installments())
        .extracting(Installment::installmentNumber, Installment::amount)
        .containsExactly(tuple(1, new BigDecimal("100.00")), tuple(3, new BigDecimal("50.00")));
    assertThat(
            jdbc.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version='8'", Boolean.class))
        .isTrue();
  }

  @Test
  void scopedSummaryPaginationUsesOnlyCompanyAndSupportsBothIdDirections() {
    var one = fixtures("One");
    var two = fixtures("Two");
    var first = accounts.save(account(one, FinancialAccountType.PAYABLE, null));
    var second = accounts.save(account(one, FinancialAccountType.RECEIVABLE, one.costCenter.id()));
    var third = accounts.save(account(one, FinancialAccountType.PAYABLE, null));
    accounts.save(account(two, FinancialAccountType.PAYABLE, null));
    var asc =
        accounts.findSummaryPageByCompanyId(
            one.company.id(),
            new FinancialAccountPageQuery(0, 2, FinancialAccountPageQuery.Direction.ASC));
    var next =
        accounts.findSummaryPageByCompanyId(
            one.company.id(),
            new FinancialAccountPageQuery(1, 2, FinancialAccountPageQuery.Direction.ASC));
    assertThat(asc.data())
        .extracting(FinancialAccountSummary::id)
        .containsExactly(first.id(), second.id());
    assertThat(next.data()).extracting(FinancialAccountSummary::id).containsExactly(third.id());
    assertThat(asc.totalElements()).isEqualTo(3);
    assertThat(asc.totalPages()).isEqualTo(2);
    assertThat(
            accounts
                .findSummaryPageByCompanyId(
                    one.company.id(),
                    new FinancialAccountPageQuery(0, 3, FinancialAccountPageQuery.Direction.DESC))
                .data())
        .extracting(FinancialAccountSummary::id)
        .containsExactly(third.id(), second.id(), first.id());
    assertThat(accounts.findByCompanyIdAndId(two.company.id(), first.id())).isEmpty();
  }

  @Test
  void databaseEnforcesInstallmentUniqueAndPositiveChecks() {
    var f = fixtures("Checks");
    var saved = accounts.save(account(f, FinancialAccountType.PAYABLE, null));
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO \"installment\"(\"financialAccountId\",\"installmentNumber\",\"dueDate\",\"amount\") VALUES(?,?,CURRENT_DATE,?)",
                    saved.id(),
                    1,
                    BigDecimal.ONE))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO \"installment\"(\"financialAccountId\",\"installmentNumber\",\"dueDate\",\"amount\") VALUES(?,?,CURRENT_DATE,?)",
                    saved.id(),
                    9,
                    BigDecimal.ZERO))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void databaseRejectsNonPositiveFinancialAccountTotalAmount() {
    var f = fixtures("Total check");

    assertThatThrownBy(() -> insertFinancialAccount(f, "PAYABLE", BigDecimal.ZERO, "DRAFT", null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void databaseRejectsNonPositiveInstallmentNumber() {
    var f = fixtures("Number check");
    var saved = accounts.save(account(f, FinancialAccountType.PAYABLE, null));

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO \"installment\"(\"financialAccountId\",\"installmentNumber\",\"dueDate\",\"amount\") VALUES(?,?,CURRENT_DATE,?)",
                    saved.id(),
                    0,
                    BigDecimal.ONE))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void databaseRejectsInvalidFinancialAccountType() {
    var f = fixtures("Type check");

    assertThatThrownBy(() -> insertFinancialAccount(f, "INVALID", BigDecimal.TEN, "DRAFT", null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void databaseRejectsInvalidFinancialAccountStatus() {
    var f = fixtures("Status check");

    assertThatThrownBy(() -> insertFinancialAccount(f, "PAYABLE", BigDecimal.TEN, "INVALID", null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void failedInstallmentRollsBackParentAndValidSibling() {
    var f = fixtures("Atomic rollback");
    var entity = new FinancialAccountJpaEntity(account(f, FinancialAccountType.PAYABLE, null));
    ReflectionTestUtils.setField(entity.installments().get(1), "installmentNumber", 0);
    var transaction = new TransactionTemplate(transactionManager);

    assertThatThrownBy(
            () ->
                transaction.executeWithoutResult(
                    ignored -> springDataAccounts.saveAndFlush(entity)))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM \"financialAccount\" WHERE \"companyId\"=?",
                Long.class,
                f.company.id()))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM \"installment\"", Long.class)).isZero();
  }

  @Test
  void summaryPageUsesConstantQueriesWithoutFetchingInstallmentCollections() {
    var f = fixtures("Summary shape");
    accounts.save(account(f, FinancialAccountType.PAYABLE, null));
    accounts.save(account(f, FinancialAccountType.RECEIVABLE, f.costCenter.id()));
    accounts.save(account(f, FinancialAccountType.PAYABLE, null));
    var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();

    var result =
        accounts.findSummaryPageByCompanyId(
            f.company.id(),
            new FinancialAccountPageQuery(0, 2, FinancialAccountPageQuery.Direction.ASC));

    assertThat(result.data()).hasSize(2);
    assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    assertThat(statistics.getCollectionFetchCount()).isZero();
  }

  @Test
  void scalarForeignKeysAndNoCascadeProtectReferencedRows() {
    var f = fixtures("Foreign keys");
    var saved = accounts.save(account(f, FinancialAccountType.PAYABLE, f.costCenter.id()));
    for (String sql :
        List.of(
            "DELETE FROM \"company\" WHERE \"id\"=" + f.company.id(),
            "DELETE FROM \"branch\" WHERE \"id\"=" + f.branch.id(),
            "DELETE FROM \"partner\" WHERE \"id\"=" + f.partner.id(),
            "DELETE FROM \"category\" WHERE \"id\"=" + f.category.id(),
            "DELETE FROM \"costCenter\" WHERE \"id\"=" + f.costCenter.id(),
            "DELETE FROM \"financialAccount\" WHERE \"id\"=" + saved.id())) {
      assertThatThrownBy(() -> jdbc.update(sql))
          .isInstanceOf(DataIntegrityViolationException.class);
    }
    assertThat(accounts.findByCompanyIdAndId(f.company.id(), saved.id())).isPresent();
  }

  @Test
  void databaseRejectsEveryInvalidScalarForeignKey() {
    var f = fixtures("Invalid FK");
    String base =
        "INSERT INTO \"financialAccount\"(\"companyId\",\"branchId\",\"type\",\"partnerId\",\"categoryId\",\"costCenterId\",\"issueDate\",\"totalAmount\",\"status\") VALUES(?,?,?,?,?,?,CURRENT_DATE,10.00,'DRAFT')";
    for (Object[] ids :
        List.of(
            new Object[] {999999L, f.branch.id(), "PAYABLE", f.partner.id(), f.category.id(), null},
            new Object[] {
              f.company.id(), 999999L, "PAYABLE", f.partner.id(), f.category.id(), null
            },
            new Object[] {f.company.id(), f.branch.id(), "PAYABLE", 999999L, f.category.id(), null},
            new Object[] {f.company.id(), f.branch.id(), "PAYABLE", f.partner.id(), 999999L, null},
            new Object[] {
              f.company.id(), f.branch.id(), "PAYABLE", f.partner.id(), f.category.id(), 999999L
            })) {
      assertThatThrownBy(() -> jdbc.update(base, ids))
          .isInstanceOf(DataIntegrityViolationException.class);
    }
  }

  private Fixtures fixtures(String suffix) {
    var company = companies.save(Company.create("Company " + suffix));
    var branch = branches.save(Branch.create(company.id(), "Branch"));
    String document = suffix.equals("Two") ? "11144477735" : "52998224725";
    var partner =
        partners.save(
            Partner.create(
                "Partner " + suffix,
                Document.of(document),
                Set.of(PartnerRole.CUSTOMER, PartnerRole.SUPPLIER)));
    var category = categories.save(Category.create(company.id(), "Category"));
    var cost = costCenters.save(CostCenter.create(company.id(), "Cost"));
    return new Fixtures(company, branch, partner, category, cost);
  }

  private long insertFinancialAccount(
      Fixtures f, String type, BigDecimal totalAmount, String status, Long costCenterId) {
    return jdbc.queryForObject(
        "INSERT INTO \"financialAccount\"(\"companyId\",\"branchId\",\"type\",\"partnerId\",\"categoryId\",\"costCenterId\",\"issueDate\",\"totalAmount\",\"status\") VALUES(?,?,?,?,?,?,CURRENT_DATE,?,?) RETURNING \"id\"",
        Long.class,
        f.company.id(),
        f.branch.id(),
        type,
        f.partner.id(),
        f.category.id(),
        costCenterId,
        totalAmount,
        status);
  }

  private FinancialAccount account(Fixtures f, FinancialAccountType type, Long costCenterId) {
    return FinancialAccount.create(
        f.company.id(),
        f.branch.id(),
        type,
        f.partner.id(),
        f.category.id(),
        costCenterId,
        LocalDate.of(2026, 8, 21),
        new BigDecimal("150.00"),
        List.of(
            Installment.create(1, LocalDate.of(2026, 9, 10), new BigDecimal("100.00")),
            Installment.create(3, LocalDate.of(2026, 10, 11), new BigDecimal("50.00"))));
  }

  private record Fixtures(
      Company company, Branch branch, Partner partner, Category category, CostCenter costCenter) {}
}
