package com.financeiro.company.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.financeiro.company.application.*;
import com.financeiro.company.domain.*;
import com.financeiro.integration.support.PostgresIntegrationTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("it")
@Import(PostgresIntegrationTestConfiguration.class)
class CompanyBranchPersistenceIT {
  @Autowired CompanyRepository companies;
  @Autowired BranchRepository branches;
  @Autowired JdbcTemplate jdbc;

  private static PageQuery page(
      int page, int size, PageQuery.SortField field, PageQuery.SortDirection direction) {
    return new PageQuery(page, size, field, direction);
  }

  @Test
  @Transactional
  void generatesIdentitiesAllowsDuplicatesAndMapsQuotedCompanyId() {
    Company first = companies.save(Company.create("Same"));
    Company second = companies.save(Company.create("Same"));
    Branch a = branches.save(Branch.create(first.id(), "Same"));
    Branch b = branches.save(Branch.create(first.id(), "Same"));
    assertThat(first.id()).isPositive();
    assertThat(second.id()).isGreaterThan(first.id());
    assertThat(a.id()).isPositive();
    assertThat(b.id()).isGreaterThan(a.id());
    assertThat(
            jdbc.queryForObject(
                "SELECT \"companyId\" FROM \"branch\" WHERE \"id\"=?", Long.class, a.id()))
        .isEqualTo(first.id());
  }

  @Test
  void databaseRejectsNonexistentCompanyForeignKey() {
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO \"branch\" (\"companyId\",\"name\") VALUES (?,?)",
                    999999L,
                    "Orphan"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void foreignKeyPreventsCascadeDelete() {
    Company company = companies.save(Company.create("Protected"));
    branches.save(Branch.create(company.id(), "Branch"));
    assertThatThrownBy(() -> jdbc.update("DELETE FROM \"company\" WHERE \"id\"=?", company.id()))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM \"branch\" WHERE \"companyId\"=?",
                Integer.class,
                company.id()))
        .isEqualTo(1);
  }

  @Test
  @Transactional
  void paginatesAndSortsCompaniesInEveryAllowedOrder() {
    jdbc.update("DELETE FROM \"branch\"");
    jdbc.update("DELETE FROM \"company\"");
    Company delta = companies.save(Company.create("Delta"));
    Company alpha = companies.save(Company.create("Alpha"));
    Company charlie = companies.save(Company.create("Charlie"));
    Company bravo = companies.save(Company.create("Bravo"));

    assertCompanyPage(
        page(0, 2, PageQuery.SortField.ID, PageQuery.SortDirection.ASC), 4, 2, "Delta", "Alpha");
    assertCompanyPage(
        page(1, 2, PageQuery.SortField.ID, PageQuery.SortDirection.ASC), 4, 2, "Charlie", "Bravo");
    assertCompanyPage(
        page(0, 2, PageQuery.SortField.ID, PageQuery.SortDirection.DESC), 4, 2, "Bravo", "Charlie");
    assertCompanyPage(
        page(1, 2, PageQuery.SortField.ID, PageQuery.SortDirection.DESC), 4, 2, "Alpha", "Delta");
    assertCompanyPage(
        page(0, 2, PageQuery.SortField.NAME, PageQuery.SortDirection.ASC), 4, 2, "Alpha", "Bravo");
    assertCompanyPage(
        page(1, 2, PageQuery.SortField.NAME, PageQuery.SortDirection.ASC),
        4,
        2,
        "Charlie",
        "Delta");
    assertCompanyPage(
        page(0, 2, PageQuery.SortField.NAME, PageQuery.SortDirection.DESC),
        4,
        2,
        "Delta",
        "Charlie");
    assertCompanyPage(
        page(1, 2, PageQuery.SortField.NAME, PageQuery.SortDirection.DESC), 4, 2, "Bravo", "Alpha");
    assertThat(delta.id()).isLessThan(alpha.id());
    assertThat(alpha.id()).isLessThan(charlie.id());
    assertThat(charlie.id()).isLessThan(bravo.id());
  }

  @Test
  @Transactional
  void paginatesAndSortsBranchesWithinCompanyScope() {
    Company scoped = companies.save(Company.create("Scoped"));
    Company other = companies.save(Company.create("Other"));
    Branch delta = branches.save(Branch.create(scoped.id(), "Delta"));
    Branch alpha = branches.save(Branch.create(scoped.id(), "Alpha"));
    Branch charlie = branches.save(Branch.create(scoped.id(), "Charlie"));
    Branch bravo = branches.save(Branch.create(scoped.id(), "Bravo"));
    Branch foreign = branches.save(Branch.create(other.id(), "Foreign"));

    assertBranchPage(
        scoped.id(),
        page(0, 2, PageQuery.SortField.ID, PageQuery.SortDirection.ASC),
        4,
        2,
        "Delta",
        "Alpha");
    assertBranchPage(
        scoped.id(),
        page(1, 2, PageQuery.SortField.ID, PageQuery.SortDirection.ASC),
        4,
        2,
        "Charlie",
        "Bravo");
    assertBranchPage(
        scoped.id(),
        page(0, 2, PageQuery.SortField.ID, PageQuery.SortDirection.DESC),
        4,
        2,
        "Bravo",
        "Charlie");
    assertBranchPage(
        scoped.id(),
        page(1, 2, PageQuery.SortField.ID, PageQuery.SortDirection.DESC),
        4,
        2,
        "Alpha",
        "Delta");
    assertBranchPage(
        scoped.id(),
        page(0, 2, PageQuery.SortField.NAME, PageQuery.SortDirection.ASC),
        4,
        2,
        "Alpha",
        "Bravo");
    assertBranchPage(
        scoped.id(),
        page(1, 2, PageQuery.SortField.NAME, PageQuery.SortDirection.ASC),
        4,
        2,
        "Charlie",
        "Delta");
    assertBranchPage(
        scoped.id(),
        page(0, 2, PageQuery.SortField.NAME, PageQuery.SortDirection.DESC),
        4,
        2,
        "Delta",
        "Charlie");
    assertBranchPage(
        scoped.id(),
        page(1, 2, PageQuery.SortField.NAME, PageQuery.SortDirection.DESC),
        4,
        2,
        "Bravo",
        "Alpha");
    assertThat(branches.findByCompanyIdAndId(scoped.id(), delta.id())).isPresent();
    assertThat(branches.findByCompanyIdAndId(other.id(), delta.id())).isEmpty();
    assertThat(
            branches
                .findPageByCompanyId(
                    scoped.id(), page(0, 100, PageQuery.SortField.ID, PageQuery.SortDirection.ASC))
                .data())
        .extracting(Branch::id)
        .doesNotContain(foreign.id());
    assertThat(delta.id()).isLessThan(alpha.id());
    assertThat(alpha.id()).isLessThan(charlie.id());
    assertThat(charlie.id()).isLessThan(bravo.id());
  }

  @Test
  void flywayAppliedV4() {
    assertThat(
            jdbc.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version='4'", Boolean.class))
        .isTrue();
  }

  private void assertCompanyPage(
      PageQuery query, long totalElements, int totalPages, String... names) {
    PageResult<Company> result = companies.findPage(query);
    assertThat(result.data()).extracting(Company::name).containsExactly(names);
    assertThat(result.page()).isEqualTo(query.page());
    assertThat(result.size()).isEqualTo(query.size());
    assertThat(result.totalElements()).isEqualTo(totalElements);
    assertThat(result.totalPages()).isEqualTo(totalPages);
  }

  private void assertBranchPage(
      Long companyId, PageQuery query, long totalElements, int totalPages, String... names) {
    PageResult<Branch> result = branches.findPageByCompanyId(companyId, query);
    assertThat(result.data()).extracting(Branch::name).containsExactly(names);
    assertThat(result.page()).isEqualTo(query.page());
    assertThat(result.size()).isEqualTo(query.size());
    assertThat(result.totalElements()).isEqualTo(totalElements);
    assertThat(result.totalPages()).isEqualTo(totalPages);
  }
}
