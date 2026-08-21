package com.financeiro.category.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.financeiro.category.application.CategoryRepository;
import com.financeiro.category.domain.Category;
import com.financeiro.company.application.*;
import com.financeiro.company.domain.Company;
import com.financeiro.costcenter.application.CostCenterRepository;
import com.financeiro.costcenter.domain.CostCenter;
import com.financeiro.integration.support.PostgresIntegrationTestConfiguration;
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
class CategoryCostCenterPersistenceIT {
  @Autowired CompanyRepository companies;
  @Autowired CategoryRepository categories;
  @Autowired CostCenterRepository costCenters;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    jdbc.update("DELETE FROM \"costCenter\"");
    jdbc.update("DELETE FROM \"category\"");
    jdbc.update("DELETE FROM \"branch\"");
    jdbc.update("DELETE FROM \"company\"");
  }

  @AfterEach
  void cleanChildren() {
    jdbc.update("DELETE FROM \"costCenter\"");
    jdbc.update("DELETE FROM \"category\"");
  }

  private PageQuery page(
      int page, int size, PageQuery.SortField field, PageQuery.SortDirection direction) {
    return new PageQuery(page, size, field, direction);
  }

  @Test
  void v6IdentityQuotedMappingsDuplicatesLifecycleAndConstraints() {
    var company = companies.save(Company.create("Company"));
    var category = categories.save(Category.create(company.id(), " Same "));
    var duplicate = categories.save(Category.create(company.id(), "Same"));
    var center = costCenters.save(CostCenter.create(company.id(), " Same "));
    var centerDuplicate = costCenters.save(CostCenter.create(company.id(), "Same"));
    assertThat(category.id()).isPositive();
    assertThat(duplicate.id()).isGreaterThan(category.id());
    assertThat(center.id()).isPositive();
    assertThat(centerDuplicate.id()).isGreaterThan(center.id());
    assertThat(
            jdbc.queryForObject(
                "SELECT \"companyId\" FROM \"costCenter\" WHERE \"id\"=?", Long.class, center.id()))
        .isEqualTo(company.id());
    category.deactivate();
    center.deactivate();
    categories.save(category);
    costCenters.save(center);
    assertThat(categories.findByCompanyIdAndId(company.id(), category.id()).orElseThrow().active())
        .isFalse();
    assertThat(costCenters.findByCompanyIdAndId(company.id(), center.id()).orElseThrow().active())
        .isFalse();
    assertThat(
            jdbc.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version='6'", Boolean.class))
        .isTrue();
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO \"category\"(\"companyId\",\"name\",\"active\") VALUES(999999,'X',true)"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO \"costCenter\"(\"companyId\",\"name\",\"active\") VALUES(999999,'X',true)"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void scopedIsolationAndForeignKeysPreventCompanyDeletion() {
    var one = companies.save(Company.create("One"));
    var two = companies.save(Company.create("Two"));
    var category = categories.save(Category.create(one.id(), "One Category"));
    var center = costCenters.save(CostCenter.create(one.id(), "One Center"));
    categories.save(Category.create(two.id(), "Other Category"));
    costCenters.save(CostCenter.create(two.id(), "Other Center"));
    assertThat(categories.findByCompanyIdAndId(two.id(), category.id())).isEmpty();
    assertThat(costCenters.findByCompanyIdAndId(two.id(), center.id())).isEmpty();
    assertThat(
            categories
                .findPageByCompanyId(
                    one.id(), page(0, 20, PageQuery.SortField.ID, PageQuery.SortDirection.ASC))
                .data())
        .extracting(Category::companyId)
        .containsOnly(one.id());
    assertThat(
            costCenters
                .findPageByCompanyId(
                    one.id(), page(0, 20, PageQuery.SortField.ID, PageQuery.SortDirection.ASC))
                .data())
        .extracting(CostCenter::companyId)
        .containsOnly(one.id());
    assertThatThrownBy(() -> jdbc.update("DELETE FROM \"company\" WHERE \"id\"=?", one.id()))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(categories.findByCompanyIdAndId(one.id(), category.id())).isPresent();
    assertThat(costCenters.findByCompanyIdAndId(one.id(), center.id())).isPresent();
  }

  @Test
  void categoryPaginationAndAllSortsAreRealAndScoped() {
    var company = companies.save(Company.create("One"));
    var other = companies.save(Company.create("Other"));
    var z = categories.save(Category.create(company.id(), "Zulu"));
    var a1 = categories.save(Category.create(company.id(), "Alpha"));
    var a2 = categories.save(Category.create(company.id(), "Alpha"));
    categories.save(Category.create(other.id(), "Foreign"));
    var first =
        categories.findPageByCompanyId(
            company.id(), page(0, 2, PageQuery.SortField.ID, PageQuery.SortDirection.ASC));
    var second =
        categories.findPageByCompanyId(
            company.id(), page(1, 2, PageQuery.SortField.ID, PageQuery.SortDirection.ASC));
    assertThat(first.data()).extracting(Category::id).containsExactly(z.id(), a1.id());
    assertThat(second.data()).extracting(Category::id).containsExactly(a2.id());
    assertThat(first.totalElements()).isEqualTo(3);
    assertThat(first.totalPages()).isEqualTo(2);
    assertThat(
            categories
                .findPageByCompanyId(
                    company.id(), page(0, 3, PageQuery.SortField.ID, PageQuery.SortDirection.DESC))
                .data())
        .extracting(Category::id)
        .containsExactly(a2.id(), a1.id(), z.id());
    assertThat(
            categories
                .findPageByCompanyId(
                    company.id(), page(0, 3, PageQuery.SortField.NAME, PageQuery.SortDirection.ASC))
                .data())
        .extracting(Category::id)
        .containsExactly(a1.id(), a2.id(), z.id());
    assertThat(
            categories
                .findPageByCompanyId(
                    company.id(),
                    page(0, 3, PageQuery.SortField.NAME, PageQuery.SortDirection.DESC))
                .data())
        .extracting(Category::id)
        .containsExactly(z.id(), a2.id(), a1.id());
  }

  @Test
  void costCenterPaginationAndAllSortsAreRealAndScoped() {
    var company = companies.save(Company.create("One"));
    var other = companies.save(Company.create("Other"));
    var z = costCenters.save(CostCenter.create(company.id(), "Zulu"));
    var a1 = costCenters.save(CostCenter.create(company.id(), "Alpha"));
    var a2 = costCenters.save(CostCenter.create(company.id(), "Alpha"));
    costCenters.save(CostCenter.create(other.id(), "Foreign"));
    var first =
        costCenters.findPageByCompanyId(
            company.id(), page(0, 2, PageQuery.SortField.ID, PageQuery.SortDirection.ASC));
    var second =
        costCenters.findPageByCompanyId(
            company.id(), page(1, 2, PageQuery.SortField.ID, PageQuery.SortDirection.ASC));
    assertThat(first.data()).extracting(CostCenter::id).containsExactly(z.id(), a1.id());
    assertThat(second.data()).extracting(CostCenter::id).containsExactly(a2.id());
    assertThat(first.totalElements()).isEqualTo(3);
    assertThat(first.totalPages()).isEqualTo(2);
    assertThat(
            costCenters
                .findPageByCompanyId(
                    company.id(), page(0, 3, PageQuery.SortField.ID, PageQuery.SortDirection.DESC))
                .data())
        .extracting(CostCenter::id)
        .containsExactly(a2.id(), a1.id(), z.id());
    assertThat(
            costCenters
                .findPageByCompanyId(
                    company.id(), page(0, 3, PageQuery.SortField.NAME, PageQuery.SortDirection.ASC))
                .data())
        .extracting(CostCenter::id)
        .containsExactly(a1.id(), a2.id(), z.id());
    assertThat(
            costCenters
                .findPageByCompanyId(
                    company.id(),
                    page(0, 3, PageQuery.SortField.NAME, PageQuery.SortDirection.DESC))
                .data())
        .extracting(CostCenter::id)
        .containsExactly(z.id(), a2.id(), a1.id());
  }
}
