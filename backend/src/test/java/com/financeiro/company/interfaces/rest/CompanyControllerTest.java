package com.financeiro.company.interfaces.rest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.financeiro.company.application.*;
import com.financeiro.company.domain.Company;
import com.financeiro.interfaces.rest.error.*;
import com.financeiro.interfaces.rest.trace.TraceIdFilter;
import java.util.List;
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

@WebMvcTest(CompanyController.class)
@ActiveProfiles("test")
@Import({
  PageQueryParser.class,
  GlobalExceptionHandler.class,
  TraceIdProvider.class,
  TraceIdFilter.class
})
class CompanyControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean CreateCompany create;
  @MockitoBean GetCompany get;
  @MockitoBean ListCompanies list;

  @Test
  void createsWithLocationAndBody() throws Exception {
    when(create.execute("  Acme  ")).thenReturn(Company.rehydrate(1L, "Acme"));
    mvc.perform(
            post("/api/v1/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"  Acme  \"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/companies/1"))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Acme"));
  }

  @Test
  void getsCompanySuccessfully() throws Exception {
    when(get.execute(1L)).thenReturn(Company.rehydrate(1L, "Acme"));
    mvc.perform(get("/api/v1/companies/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Acme"));
  }

  @Test
  void listsCompaniesWithDocumentedMetadata() throws Exception {
    when(list.execute(any()))
        .thenReturn(new PageResult<>(List.of(Company.rehydrate(1L, "Acme")), 0, 20, 1, 1));
    mvc.perform(get("/api/v1/companies"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].name").value("Acme"))
        .andExpect(jsonPath("$.meta.page").value(0))
        .andExpect(jsonPath("$.meta.size").value(20))
        .andExpect(jsonPath("$.meta.totalElements").value(1))
        .andExpect(jsonPath("$.meta.totalPages").value(1));
  }

  @ParameterizedTest
  @ValueSource(strings = {"id,asc", "id,desc", "name,asc", "name,desc"})
  void acceptsEveryDocumentedSort(String sort) throws Exception {
    when(list.execute(any())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
    mvc.perform(get("/api/v1/companies").param("sort", sort)).andExpect(status().isOk());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 100})
  void acceptsDocumentedSizeBoundaries(int size) throws Exception {
    when(list.execute(any())).thenReturn(new PageResult<>(List.of(), 0, size, 0, 0));
    mvc.perform(get("/api/v1/companies").param("page", "0").param("size", Integer.toString(size)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meta.size").value(size));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "?size=0",
        "?size=101",
        "?page=-1",
        "?sort=id",
        "?sort=createdAt,asc",
        "?sort=id,up"
      })
  void rejectsInvalidPaginationAndSort(String query) throws Exception {
    mvc.perform(get("/api/v1/companies" + query))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void rejectsUnknownCompanyRequestField() throws Exception {
    mvc.perform(
            post("/api/v1/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Acme\",\"unknown\":true}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    verifyNoInteractions(create);
  }
}
