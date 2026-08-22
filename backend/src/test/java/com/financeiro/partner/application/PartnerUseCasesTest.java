package com.financeiro.partner.application;

import static org.assertj.core.api.Assertions.*;

import com.financeiro.partner.domain.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PartnerUseCasesTest {
  @Test
  void createsBothRolesAndDetectsFriendlyDuplicate() {
    Repo r = new Repo();
    Partner p =
        new CreatePartner(r)
            .execute(
                " Acme ", "529.982.247-25", Set.of(PartnerRole.CUSTOMER, PartnerRole.SUPPLIER));
    assertThat(p.active()).isTrue();
    assertThat(p.roles()).containsExactlyInAnyOrder(PartnerRole.CUSTOMER, PartnerRole.SUPPLIER);
    assertThatThrownBy(
            () ->
                new CreatePartner(r).execute("Other", "52998224725", Set.of(PartnerRole.CUSTOMER)))
        .isInstanceOf(PartnerDocumentAlreadyExistsException.class);
  }

  @Test
  void validatesInvalidPartnerBeforeDuplicateLookup() {
    Repo r = new Repo();
    r.save(Partner.create("Existing", Document.of("52998224725"), Set.of(PartnerRole.CUSTOMER)));
    assertThatThrownBy(
            () -> new CreatePartner(r).execute(" ", "52998224725", Set.of(PartnerRole.CUSTOMER)))
        .isInstanceOf(InvalidPartnerNameException.class);
    assertThatThrownBy(() -> new CreatePartner(r).execute("Valid", "52998224725", Set.of()))
        .isInstanceOf(InvalidPartnerRolesException.class);
    assertThat(r.findByDocumentCalls).isZero();
  }

  @Test
  void propagatesPersistenceRaceConflict() {
    Repo r = new Repo();
    r.failSave = true;
    assertThatThrownBy(
            () -> new CreatePartner(r).execute("A", "52998224725", Set.of(PartnerRole.CUSTOMER)))
        .isInstanceOf(PartnerDocumentAlreadyExistsException.class);
  }

  @Test
  void getsActiveInactiveAndNotFound() {
    Repo r = new Repo();
    Partner p =
        r.save(Partner.create("A", Document.of("52998224725"), Set.of(PartnerRole.CUSTOMER)));
    assertThat(new GetPartner(r).execute(p.id()).active()).isTrue();
    p.deactivate();
    r.save(p);
    assertThat(new GetPartner(r).execute(p.id()).active()).isFalse();
    assertThatThrownBy(() -> new GetPartner(r).execute(99L))
        .isInstanceOf(PartnerNotFoundException.class);
  }

  @Test
  void listsWithMetadata() {
    Repo r = new Repo();
    r.save(Partner.create("A", Document.of("52998224725"), Set.of(PartnerRole.CUSTOMER)));
    var q =
        new PartnerPageQuery(0, 20, PartnerPageQuery.SortField.ID, PartnerPageQuery.Direction.ASC);
    var result = new ListPartners(r).execute(q);
    assertThat(result.data()).hasSize(1);
    assertThat(result.totalElements()).isEqualTo(1);
    assertThat(result.totalPages()).isEqualTo(1);
  }

  @Test
  void deactivatesRepeatedlyAndRejectsMissing() {
    Repo r = new Repo();
    Partner p =
        r.save(Partner.create("A", Document.of("52998224725"), Set.of(PartnerRole.SUPPLIER)));
    assertThat(new DeactivatePartner(r).execute(p.id()).active()).isFalse();
    assertThat(new DeactivatePartner(r).execute(p.id()).active()).isFalse();
    assertThatThrownBy(() -> new DeactivatePartner(r).execute(99L))
        .isInstanceOf(PartnerNotFoundException.class);
  }

  static class Repo implements PartnerRepository {
    Map<Long, Partner> data = new HashMap<>();
    long seq;
    boolean failSave;
    int findByDocumentCalls;

    public Partner save(Partner p) {
      if (failSave) throw new PartnerDocumentAlreadyExistsException();
      Partner saved =
          p.id() == null
              ? Partner.rehydrate(++seq, p.name(), p.document(), p.roles(), p.active())
              : p;
      data.put(saved.id(), saved);
      return saved;
    }

    public Optional<Partner> findById(Long id) {
      return Optional.ofNullable(data.get(id));
    }

    public Optional<Partner> findByDocument(Document d) {
      findByDocumentCalls++;
      return data.values().stream().filter(p -> p.document().equals(d)).findFirst();
    }

    public PartnerPageResult<Partner> findPage(PartnerPageQuery q) {
      var list = data.values().stream().toList();
      return new PartnerPageResult<>(list, q.page(), q.size(), list.size(), list.isEmpty() ? 0 : 1);
    }
  }
}
