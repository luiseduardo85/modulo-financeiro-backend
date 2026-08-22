package com.financeiro.bankaccount.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.financeiro.bankaccount.application.BankAccountRepository;
import com.financeiro.bankaccount.domain.BankAccount;
import com.financeiro.company.application.*;
import com.financeiro.company.domain.Branch;
import com.financeiro.company.domain.Company;
import com.financeiro.integration.support.PostgresIntegrationTestConfiguration;
import com.financeiro.paymentmethod.application.PaymentMethodRepository;
import com.financeiro.paymentmethod.domain.PaymentMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("it")
@Import(PostgresIntegrationTestConfiguration.class)
class BankAccountPaymentMethodPersistenceIT {
  @Autowired CompanyRepository companies;
  @Autowired BranchRepository branches;
  @Autowired BankAccountRepository accounts;
  @Autowired PaymentMethodRepository methods;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    jdbc.update("DELETE FROM \"financialMovement\"");
    jdbc.update("DELETE FROM \"bankAccount\"");
    jdbc.update("DELETE FROM \"paymentMethod\"");
    jdbc.update("DELETE FROM \"costCenter\"");
    jdbc.update("DELETE FROM \"category\"");
    jdbc.update("DELETE FROM \"branch\"");
    jdbc.update("DELETE FROM \"company\"");
  }

  @AfterEach
  void cleanOwnedFixtures() {
    jdbc.update("DELETE FROM \"bankAccount\"");
    jdbc.update("DELETE FROM \"paymentMethod\"");
  }

  private PageQuery page(
      int page, int size, PageQuery.SortField field, PageQuery.SortDirection direction) {
    return new PageQuery(page, size, field, direction);
  }

  @Test
  void v7MappingsIdentitiesDuplicatesLifecycleAndForeignKeys() {
    var company = companies.save(Company.create("Company"));
    var branch = branches.save(Branch.create(company.id(), "Branch"));
    var general = accounts.save(BankAccount.create(company.id(), null, "Same"));
    var restricted = accounts.save(BankAccount.create(company.id(), branch.id(), "Same"));
    var firstMethod = methods.save(PaymentMethod.create(company.id(), "Same"));
    var secondMethod = methods.save(PaymentMethod.create(company.id(), "Same"));

    assertThat(general.id()).isPositive();
    assertThat(restricted.id()).isGreaterThan(general.id());
    assertThat(firstMethod.id()).isPositive();
    assertThat(secondMethod.id()).isGreaterThan(firstMethod.id());
    assertThat(
            jdbc.queryForObject(
                "SELECT \"branchId\" FROM \"bankAccount\" WHERE \"id\"=?",
                Long.class,
                restricted.id()))
        .isEqualTo(branch.id());
    assertThat(
            jdbc.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version='7'", Boolean.class))
        .isTrue();

    restricted.deactivate();
    firstMethod.deactivate();
    accounts.save(restricted);
    methods.save(firstMethod);
    assertThat(accounts.findByCompanyIdAndId(company.id(), restricted.id()).orElseThrow().active())
        .isFalse();
    assertThat(methods.findByCompanyIdAndId(company.id(), firstMethod.id()).orElseThrow().active())
        .isFalse();

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO \"bankAccount\"(\"companyId\",\"branchId\",\"name\",\"active\") VALUES(999999,NULL,'X',true)"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO \"bankAccount\"(\"companyId\",\"branchId\",\"name\",\"active\") VALUES(?,999999,'X',true)",
                    company.id()))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO \"paymentMethod\"(\"companyId\",\"name\",\"active\") VALUES(999999,'X',true)"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void scopedRepositoriesIsolateCompanies() {
    var one = companies.save(Company.create("One"));
    var two = companies.save(Company.create("Two"));
    var account = accounts.save(BankAccount.create(one.id(), null, "One account"));
    var method = methods.save(PaymentMethod.create(one.id(), "One method"));
    accounts.save(BankAccount.create(two.id(), null, "Other account"));
    methods.save(PaymentMethod.create(two.id(), "Other method"));

    assertThat(accounts.findByCompanyIdAndId(two.id(), account.id())).isEmpty();
    assertThat(methods.findByCompanyIdAndId(two.id(), method.id())).isEmpty();
    assertThat(
            accounts
                .findPageByCompanyId(
                    one.id(), page(0, 20, PageQuery.SortField.ID, PageQuery.SortDirection.ASC))
                .data())
        .extracting(BankAccount::companyId)
        .containsOnly(one.id());
    assertThat(
            methods
                .findPageByCompanyId(
                    one.id(), page(0, 20, PageQuery.SortField.ID, PageQuery.SortDirection.ASC))
                .data())
        .extracting(PaymentMethod::companyId)
        .containsOnly(one.id());
  }

  @Test
  void bankAccountAlonePreventsCompanyDeletion() {
    var company = companies.save(Company.create("Bank owner"));
    var account = accounts.save(BankAccount.create(company.id(), null, "Account"));

    assertThatThrownBy(() -> jdbc.update("DELETE FROM \"company\" WHERE \"id\"=?", company.id()))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(accounts.findByCompanyIdAndId(company.id(), account.id())).isPresent();
  }

  @Test
  void paymentMethodAlonePreventsCompanyDeletion() {
    var company = companies.save(Company.create("Method owner"));
    var method = methods.save(PaymentMethod.create(company.id(), "Method"));

    assertThatThrownBy(() -> jdbc.update("DELETE FROM \"company\" WHERE \"id\"=?", company.id()))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(methods.findByCompanyIdAndId(company.id(), method.id())).isPresent();
  }

  @Test
  void restrictedBankAccountPreventsBranchDeletion() {
    var company = companies.save(Company.create("Branch owner"));
    var branch = branches.save(Branch.create(company.id(), "Branch"));
    var account = accounts.save(BankAccount.create(company.id(), branch.id(), "Restricted"));

    assertThatThrownBy(() -> jdbc.update("DELETE FROM \"branch\" WHERE \"id\"=?", branch.id()))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(accounts.findByCompanyIdAndId(company.id(), account.id()))
        .get()
        .extracting(BankAccount::branchId)
        .isEqualTo(branch.id());
  }

  @Test
  void databaseReferencesBranchButApplicationOwnsSameCompanyValidationBoundary() {
    var one = companies.save(Company.create("One"));
    var two = companies.save(Company.create("Two"));
    var foreignBranch = branches.save(Branch.create(two.id(), "Foreign"));

    // Both scalar foreign keys are valid independently; CreateBankAccount prevents this
    // combination.
    int rows =
        jdbc.update(
            "INSERT INTO \"bankAccount\"(\"companyId\",\"branchId\",\"name\",\"active\") VALUES(?,?,?,true)",
            one.id(),
            foreignBranch.id(),
            "Cross-company fixture");
    assertThat(rows).isOne();
  }

  @Test
  void bankAccountPaginationAndAllSortsAreRealAndScoped() {
    var company = companies.save(Company.create("One"));
    var other = companies.save(Company.create("Other"));
    var z = accounts.save(BankAccount.create(company.id(), null, "Zulu"));
    var a1 = accounts.save(BankAccount.create(company.id(), null, "Alpha"));
    var a2 = accounts.save(BankAccount.create(company.id(), null, "Alpha"));
    accounts.save(BankAccount.create(other.id(), null, "Foreign"));
    assertPageAndSortsForAccounts(company.id(), z, a1, a2);
  }

  private void assertPageAndSortsForAccounts(
      Long companyId, BankAccount z, BankAccount a1, BankAccount a2) {
    var first =
        accounts.findPageByCompanyId(
            companyId, page(0, 2, PageQuery.SortField.ID, PageQuery.SortDirection.ASC));
    var second =
        accounts.findPageByCompanyId(
            companyId, page(1, 2, PageQuery.SortField.ID, PageQuery.SortDirection.ASC));
    assertThat(first.data()).extracting(BankAccount::id).containsExactly(z.id(), a1.id());
    assertThat(second.data()).extracting(BankAccount::id).containsExactly(a2.id());
    assertThat(first.totalElements()).isEqualTo(3);
    assertThat(first.totalPages()).isEqualTo(2);
    assertThat(
            accounts
                .findPageByCompanyId(
                    companyId, page(0, 3, PageQuery.SortField.ID, PageQuery.SortDirection.DESC))
                .data())
        .extracting(BankAccount::id)
        .containsExactly(a2.id(), a1.id(), z.id());
    assertThat(
            accounts
                .findPageByCompanyId(
                    companyId, page(0, 3, PageQuery.SortField.NAME, PageQuery.SortDirection.ASC))
                .data())
        .extracting(BankAccount::id)
        .containsExactly(a1.id(), a2.id(), z.id());
    assertThat(
            accounts
                .findPageByCompanyId(
                    companyId, page(0, 3, PageQuery.SortField.NAME, PageQuery.SortDirection.DESC))
                .data())
        .extracting(BankAccount::id)
        .containsExactly(z.id(), a2.id(), a1.id());
  }

  @Test
  void paymentMethodPaginationAndAllSortsAreRealAndScoped() {
    var company = companies.save(Company.create("One"));
    var other = companies.save(Company.create("Other"));
    var z = methods.save(PaymentMethod.create(company.id(), "Zulu"));
    var a1 = methods.save(PaymentMethod.create(company.id(), "Alpha"));
    var a2 = methods.save(PaymentMethod.create(company.id(), "Alpha"));
    methods.save(PaymentMethod.create(other.id(), "Foreign"));
    var first =
        methods.findPageByCompanyId(
            company.id(), page(0, 2, PageQuery.SortField.ID, PageQuery.SortDirection.ASC));
    var second =
        methods.findPageByCompanyId(
            company.id(), page(1, 2, PageQuery.SortField.ID, PageQuery.SortDirection.ASC));
    assertThat(first.data()).extracting(PaymentMethod::id).containsExactly(z.id(), a1.id());
    assertThat(second.data()).extracting(PaymentMethod::id).containsExactly(a2.id());
    assertThat(first.totalElements()).isEqualTo(3);
    assertThat(first.totalPages()).isEqualTo(2);
    assertThat(
            methods
                .findPageByCompanyId(
                    company.id(), page(0, 3, PageQuery.SortField.ID, PageQuery.SortDirection.DESC))
                .data())
        .extracting(PaymentMethod::id)
        .containsExactly(a2.id(), a1.id(), z.id());
    assertThat(
            methods
                .findPageByCompanyId(
                    company.id(), page(0, 3, PageQuery.SortField.NAME, PageQuery.SortDirection.ASC))
                .data())
        .extracting(PaymentMethod::id)
        .containsExactly(a1.id(), a2.id(), z.id());
    assertThat(
            methods
                .findPageByCompanyId(
                    company.id(),
                    page(0, 3, PageQuery.SortField.NAME, PageQuery.SortDirection.DESC))
                .data())
        .extracting(PaymentMethod::id)
        .containsExactly(z.id(), a2.id(), a1.id());
  }
}
