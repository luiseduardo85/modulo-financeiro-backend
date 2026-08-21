package com.financeiro.category.interfaces.rest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.financeiro.category.application.*;
import com.financeiro.category.domain.Category;
import com.financeiro.company.application.PageResult;
import com.financeiro.company.interfaces.rest.PageQueryParser;
import com.financeiro.interfaces.rest.error.*;
import com.financeiro.interfaces.rest.trace.TraceIdFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CategoryController.class)
@ActiveProfiles("test")
@Import({
  PageQueryParser.class,
  GlobalExceptionHandler.class,
  TraceIdProvider.class,
  TraceIdFilter.class
})
class CategoryControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean CreateCategory create;
  @MockitoBean GetCategory get;
  @MockitoBean ListCategoriesByCompany list;
  @MockitoBean DeactivateCategory deactivate;

  @Test
  void allEndpointsFollowContract() throws Exception {
    var value = Category.rehydrate(7L, 3L, "Operational", true);
    when(create.execute(3L, "Operational")).thenReturn(value);
    when(get.execute(3L, 7L)).thenReturn(value);
    when(list.execute(eq(3L), any())).thenReturn(new PageResult<>(List.of(value), 0, 1, 1, 1));
    when(deactivate.execute(3L, 7L)).thenReturn(Category.rehydrate(7L, 3L, "Operational", false));
    mvc.perform(
            post("/api/v1/companies/3/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Operational\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/companies/3/categories/7"));
    mvc.perform(get("/api/v1/companies/3/categories/7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));
    mvc.perform(get("/api/v1/companies/3/categories?page=0&size=1&sort=name,desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meta.totalElements").value(1));
    mvc.perform(post("/api/v1/companies/3/categories/7/deactivate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
  }

  @Test
  void rejectsUnknownAndInvalidPagination() throws Exception {
    mvc.perform(
            post("/api/v1/companies/3/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\",\"companyId\":3}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    mvc.perform(get("/api/v1/companies/3/categories?size=101"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void crossCompanyMissingDoesNotLeak() throws Exception {
    when(get.execute(2L, 7L)).thenThrow(new CategoryNotFoundException(2L, 7L));
    mvc.perform(get("/api/v1/companies/2/categories/7"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"))
        .andExpect(jsonPath("$.traceId").exists());
  }
}
