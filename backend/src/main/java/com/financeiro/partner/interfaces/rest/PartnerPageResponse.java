package com.financeiro.partner.interfaces.rest;

import java.util.List;

public record PartnerPageResponse(List<PartnerResponse> data, PartnerPageMetaResponse meta) {}
