package com.financeiro.partner.application;

import com.financeiro.partner.domain.*;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatePartner {
  private final PartnerRepository repository;

  public CreatePartner(PartnerRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public Partner execute(String name, String document, Set<PartnerRole> roles) {
    Document value = Document.of(document);
    Partner partner = Partner.create(name, value, roles);
    if (repository.findByDocument(partner.document()).isPresent()) {
      throw new PartnerDocumentAlreadyExistsException();
    }
    return repository.save(partner);
  }
}
