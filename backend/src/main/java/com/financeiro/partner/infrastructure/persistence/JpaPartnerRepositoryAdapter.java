package com.financeiro.partner.infrastructure.persistence;

import com.financeiro.partner.application.*;
import com.financeiro.partner.domain.*;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public class JpaPartnerRepositoryAdapter implements PartnerRepository {
  private static final String DOCUMENT_CONSTRAINT = "ukPartnerDocument";
  private final SpringDataPartnerRepository repository;

  public JpaPartnerRepositoryAdapter(SpringDataPartnerRepository repository) {
    this.repository = repository;
  }

  public Partner save(Partner p) {
    try {
      return toDomain(repository.saveAndFlush(toEntity(p)));
    } catch (DataIntegrityViolationException e) {
      if (hasDocumentConstraint(e)) throw new PartnerDocumentAlreadyExistsException();
      throw e;
    }
  }

  public Optional<Partner> findById(Long id) {
    return repository.findById(id).map(JpaPartnerRepositoryAdapter::toDomain);
  }

  public Optional<Partner> findByDocument(Document d) {
    return repository.findByDocument(d.value()).map(JpaPartnerRepositoryAdapter::toDomain);
  }

  public PartnerPageResult<Partner> findPage(PartnerPageQuery q) {
    Page<PartnerJpaEntity> page = repository.findAll(pageable(q));
    return new PartnerPageResult<>(
        page.map(JpaPartnerRepositoryAdapter::toDomain).getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }

  private static Pageable pageable(PartnerPageQuery q) {
    Sort.Direction d =
        q.direction() == PartnerPageQuery.Direction.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
    Sort sort =
        q.field() == PartnerPageQuery.SortField.ID
            ? Sort.by(d, "id")
            : Sort.by(d, "name").and(Sort.by(d, "id"));
    return PageRequest.of(q.page(), q.size(), sort);
  }

  private static PartnerJpaEntity toEntity(Partner p) {
    return new PartnerJpaEntity(
        p.id(),
        p.name(),
        p.document().value(),
        p.roles().contains(PartnerRole.CUSTOMER),
        p.roles().contains(PartnerRole.SUPPLIER),
        p.active());
  }

  private static Partner toDomain(PartnerJpaEntity e) {
    EnumSet<PartnerRole> roles = EnumSet.noneOf(PartnerRole.class);
    if (e.customer()) roles.add(PartnerRole.CUSTOMER);
    if (e.supplier()) roles.add(PartnerRole.SUPPLIER);
    return Partner.rehydrate(e.id(), e.name(), Document.of(e.document()), roles, e.active());
  }

  private static boolean hasDocumentConstraint(Throwable error) {
    for (Throwable current = error; current != null; current = current.getCause())
      if (current instanceof org.hibernate.exception.ConstraintViolationException c
          && DOCUMENT_CONSTRAINT.equals(c.getConstraintName())) return true;
    return false;
  }
}
