package com.financeiro.partner.application;

import com.financeiro.partner.domain.Partner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListPartners {
  private final PartnerRepository repository;

  public ListPartners(PartnerRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public PartnerPageResult<Partner> execute(PartnerPageQuery query) {
    return repository.findPage(query);
  }
}
