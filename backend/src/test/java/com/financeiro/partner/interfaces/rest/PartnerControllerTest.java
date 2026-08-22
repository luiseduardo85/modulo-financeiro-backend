package com.financeiro.partner.interfaces.rest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.financeiro.interfaces.rest.error.*;
import com.financeiro.interfaces.rest.trace.*;
import com.financeiro.partner.application.*;
import com.financeiro.partner.domain.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PartnerController.class)
@ActiveProfiles("test")
@Import({
  PartnerPageQueryParser.class,
  GlobalExceptionHandler.class,
  TraceIdProvider.class,
  TraceIdFilter.class
})
class PartnerControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean CreatePartner create;
  @MockitoBean GetPartner getPartner;
  @MockitoBean ListPartners list;
  @MockitoBean DeactivatePartner deactivate;

  private Partner partner(boolean active, Set<PartnerRole> roles) {
    return Partner.rehydrate(
        1L, "Fornecedor Exemplo", Document.of("04252011000110"), roles, active);
  }

  @Test
  void createsWithoutIdempotencyHeaderAndReturnsCanonicalContract() throws Exception {
    when(create.execute(eq("Fornecedor Exemplo"), eq("04.252.011/0001-10"), anySet()))
        .thenReturn(partner(true, Set.of(PartnerRole.SUPPLIER)));
    mvc.perform(
            post("/api/v1/partners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Fornecedor Exemplo\",\"document\":\"04.252.011/0001-10\",\"roles\":[\"SUPPLIER\"]}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/partners/1"))
        .andExpect(jsonPath("$.document").value("04252011000110"))
        .andExpect(jsonPath("$.documentType").value("CNPJ"))
        .andExpect(jsonPath("$.roles[0]").value("SUPPLIER"))
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void returnsCanonicalUppercaseAlphanumericCnpj() throws Exception {
    Partner p =
        Partner.rehydrate(
            2L,
            "Alfanumerico",
            Document.of("00.000.000/e08g-12"),
            Set.of(PartnerRole.CUSTOMER),
            true);
    when(create.execute(eq("Alfanumerico"), eq("00.000.000/e08g-12"), anySet())).thenReturn(p);
    mvc.perform(
            post("/api/v1/partners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Alfanumerico\",\"document\":\"00.000.000/e08g-12\",\"roles\":[\"CUSTOMER\"]}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.document").value("00000000E08G12"))
        .andExpect(jsonPath("$.documentType").value("CNPJ"));
  }

  @Test
  void returnsBothRolesInDeclarationOrder() throws Exception {
    when(getPartner.execute(1L))
        .thenReturn(partner(true, Set.of(PartnerRole.SUPPLIER, PartnerRole.CUSTOMER)));
    mvc.perform(get("/api/v1/partners/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"))
        .andExpect(jsonPath("$.roles[1]").value("SUPPLIER"));
  }

  @Test
  void listsAndDeactivates() throws Exception {
    when(list.execute(any()))
        .thenReturn(
            new PartnerPageResult<>(
                List.of(partner(true, Set.of(PartnerRole.CUSTOMER))), 0, 20, 1, 1));
    when(deactivate.execute(1L)).thenReturn(partner(false, Set.of(PartnerRole.CUSTOMER)));
    mvc.perform(get("/api/v1/partners"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meta.totalElements").value(1));
    mvc.perform(post("/api/v1/partners/1/deactivate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
  }

  @Test
  void mapsInvalidDuplicateAndNotFoundWithTraceId() throws Exception {
    when(create.execute(anyString(), eq("bad"), anySet()))
        .thenThrow(new InvalidPartnerDocumentException());
    when(create.execute(anyString(), eq("52998224725"), anySet()))
        .thenThrow(new PartnerDocumentAlreadyExistsException());
    when(getPartner.execute(99L)).thenThrow(new PartnerNotFoundException(99L));
    mvc.perform(
            post("/api/v1/partners")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"A\",\"document\":\"bad\",\"roles\":[\"CUSTOMER\"]}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("INVALID_PARTNER_DOCUMENT"))
        .andExpect(jsonPath("$.traceId").isNotEmpty());
    mvc.perform(
            post("/api/v1/partners")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"A\",\"document\":\"52998224725\",\"roles\":[\"CUSTOMER\"]}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("PARTNER_DOCUMENT_ALREADY_EXISTS"));
    mvc.perform(get("/api/v1/partners/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PARTNER_NOT_FOUND"));
  }

  @Test
  void rejectsEmptyRolesAndUnknownFields() throws Exception {
    when(create.execute(anyString(), anyString(), eq(Set.of())))
        .thenThrow(new InvalidPartnerRolesException());
    mvc.perform(
            post("/api/v1/partners")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"A\",\"document\":\"52998224725\",\"roles\":[]}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    for (String field : new String[] {"id", "active", "documentType", "companyId", "unknown"})
      mvc.perform(
              post("/api/v1/partners")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      "{\"name\":\"A\",\"document\":\"52998224725\",\"roles\":[\"CUSTOMER\"],\""
                          + field
                          + "\":1}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"id,asc", "id,desc", "name,asc", "name,desc"})
  void acceptsSorts(String sort) throws Exception {
    when(list.execute(any())).thenReturn(new PartnerPageResult<>(List.of(), 0, 20, 0, 0));
    mvc.perform(get("/api/v1/partners").param("sort", sort)).andExpect(status().isOk());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "?size=0",
        "?size=101",
        "?page=-1",
        "?sort=id",
        "?sort=document,asc",
        "?sort=id,up"
      })
  void rejectsInvalidPagination(String query) throws Exception {
    mvc.perform(get("/api/v1/partners" + query))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }
}
