package com.financeiro.partner.application;

import com.financeiro.partner.domain.Partner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeactivatePartner {
  private final PartnerRepository repository;

  public DeactivatePartner(PartnerRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public Partner execute(Long id) {
    Partner p = repository.findById(id).orElseThrow(() -> new PartnerNotFoundException(id));
    p.deactivate();
    return repository.save(p);
  }
}
