package com.financeiro.partner.application;

import com.financeiro.partner.domain.Document;
import com.financeiro.partner.domain.Partner;
import java.util.Optional;

public interface PartnerRepository {
  Partner save(Partner partner);

  Optional<Partner> findById(Long id);

  Optional<Partner> findByDocument(Document document);

  PartnerPageResult<Partner> findPage(PartnerPageQuery query);
}
