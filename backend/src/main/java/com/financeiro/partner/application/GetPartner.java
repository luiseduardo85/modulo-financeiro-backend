package com.financeiro.partner.application;

import com.financeiro.partner.domain.Partner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPartner {
  private final PartnerRepository repository;

  public GetPartner(PartnerRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public Partner execute(Long id) {
    return repository.findById(id).orElseThrow(() -> new PartnerNotFoundException(id));
  }
}
