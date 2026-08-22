package com.financeiro.partner.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.financeiro.integration.support.PostgresIntegrationTestConfiguration;
import com.financeiro.partner.application.*;
import com.financeiro.partner.domain.*;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
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
class PartnerPersistenceIT {
  @Autowired PartnerRepository partners;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void cleanPartnerTable() {
    jdbc.update("DELETE FROM \"partner\"");
  }

  private Partner save(String name, String document, Set<PartnerRole> roles) {
    return partners.save(Partner.create(name, Document.of(document), roles));
  }

  private PartnerPageQuery page(
      int page, int size, PartnerPageQuery.SortField field, PartnerPageQuery.Direction direction) {
    return new PartnerPageQuery(page, size, field, direction);
  }

  @Test
  @Transactional
  void appliesV5GeneratesIdentityPersistsCanonicalRolesAndActive() {
    Partner p =
        save(" Acme ", "04.252.011/0001-10", Set.of(PartnerRole.CUSTOMER, PartnerRole.SUPPLIER));
    assertThat(p.id()).isPositive();
    assertThat(p.document().value()).isEqualTo("04252011000110");
    MapAssert.row(jdbc, p.id())
        .containsEntry("customer", true)
        .containsEntry("supplier", true)
        .containsEntry("active", true);
    assertThat(
            jdbc.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version='5'", Boolean.class))
        .isTrue();
  }

  @Test
  void persistsCanonicalAlphanumericCnpjAndDatabaseRejectsNoncanonicalForms() {
    Partner p = save("Alpha", "00.000.000/e08g-12", Set.of(PartnerRole.CUSTOMER));
    assertThat(
            jdbc.queryForObject(
                "SELECT \"document\" FROM \"partner\" WHERE \"id\"=?", String.class, p.id()))
        .isEqualTo("00000000E08G12");
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO \"partner\"(\"name\",\"document\",\"customer\",\"supplier\",\"active\") VALUES('Lower','00000000e08g12',true,false,true)"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO \"partner\"(\"name\",\"document\",\"customer\",\"supplier\",\"active\") VALUES('Bad DV','00000000E08G1A',true,false,true)"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @Transactional
  void uniqueDocumentIsGlobalAndSpecificallyTranslated() {
    save("One", "52998224725", Set.of(PartnerRole.CUSTOMER));
    assertThatThrownBy(() -> save("Two", "529.982.247-25", Set.of(PartnerRole.SUPPLIER)))
        .isInstanceOf(PartnerDocumentAlreadyExistsException.class)
        .hasMessageNotContaining("529")
        .hasMessageNotContaining("ukPartnerDocument");
  }

  @Test
  void databaseRejectsZeroRolesAndAllowsDuplicateNames() {
    Long first =
        jdbc.queryForObject(
            "INSERT INTO \"partner\"(\"name\",\"document\",\"customer\",\"supplier\",\"active\") VALUES('Same','52998224725',true,false,true) RETURNING \"id\"",
            Long.class);
    Long second =
        jdbc.queryForObject(
            "INSERT INTO \"partner\"(\"name\",\"document\",\"customer\",\"supplier\",\"active\") VALUES('Same','11144477735',false,true,true) RETURNING \"id\"",
            Long.class);
    assertThat(first).isNotEqualTo(second);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO \"partner\"(\"name\",\"document\",\"customer\",\"supplier\",\"active\") VALUES('Bad','93541134780',false,false,true)"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @Transactional
  void inactivePartnerRemainsFindable() {
    Partner p = save("A", "93541134780", Set.of(PartnerRole.SUPPLIER));
    p.deactivate();
    partners.save(p);
    assertThat(partners.findById(p.id())).get().extracting(Partner::active).isEqualTo(false);
  }

  @Test
  @Transactional
  void paginatesAndSortsAllAllowedWays() {
    jdbc.update("DELETE FROM \"partner\"");
    Partner delta = save("Delta", "52998224725", Set.of(PartnerRole.CUSTOMER));
    Partner alpha = save("Alpha", "11144477735", Set.of(PartnerRole.SUPPLIER));
    Partner charlie =
        save("Charlie", "93541134780", Set.of(PartnerRole.CUSTOMER, PartnerRole.SUPPLIER));
    Partner bravo = save("Bravo", "39053344705", Set.of(PartnerRole.CUSTOMER));
    assertPage(
        page(0, 2, PartnerPageQuery.SortField.ID, PartnerPageQuery.Direction.ASC),
        "Delta",
        "Alpha");
    assertPage(
        page(1, 2, PartnerPageQuery.SortField.ID, PartnerPageQuery.Direction.ASC),
        "Charlie",
        "Bravo");
    assertPage(
        page(0, 2, PartnerPageQuery.SortField.ID, PartnerPageQuery.Direction.DESC),
        "Bravo",
        "Charlie");
    assertPage(
        page(0, 2, PartnerPageQuery.SortField.NAME, PartnerPageQuery.Direction.ASC),
        "Alpha",
        "Bravo");
    assertPage(
        page(0, 2, PartnerPageQuery.SortField.NAME, PartnerPageQuery.Direction.DESC),
        "Delta",
        "Charlie");
    assertThat(delta.id()).isLessThan(alpha.id());
    assertThat(alpha.id()).isLessThan(charlie.id());
    assertThat(charlie.id()).isLessThan(bravo.id());
  }

  @Test
  void hasNoCompanyColumnOrForeignKey() {
    Integer columns =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns WHERE table_name='partner' AND column_name='companyId'",
            Integer.class);
    Integer fks =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_name='partner' AND constraint_type='FOREIGN KEY'",
            Integer.class);
    assertThat(columns).isZero();
    assertThat(fks).isZero();
  }

  private void assertPage(PartnerPageQuery q, String... names) {
    var r = partners.findPage(q);
    assertThat(r.data()).extracting(Partner::name).containsExactly(names);
    assertThat(r.page()).isEqualTo(q.page());
    assertThat(r.size()).isEqualTo(2);
    assertThat(r.totalElements()).isEqualTo(4);
    assertThat(r.totalPages()).isEqualTo(2);
  }

  private static final class MapAssert {
    static org.assertj.core.api.MapAssert<String, Object> row(JdbcTemplate jdbc, Long id) {
      return assertThat(
          jdbc.queryForMap(
              "SELECT \"customer\",\"supplier\",\"active\" FROM \"partner\" WHERE \"id\"=?", id));
    }
  }
}
