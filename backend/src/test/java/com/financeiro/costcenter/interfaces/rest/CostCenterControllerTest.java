package com.financeiro.costcenter.interfaces.rest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.financeiro.company.application.PageResult;
import com.financeiro.company.interfaces.rest.PageQueryParser;
import com.financeiro.costcenter.application.*;
import com.financeiro.costcenter.domain.CostCenter;
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

@WebMvcTest(CostCenterController.class)
@ActiveProfiles("test")
@Import({
  PageQueryParser.class,
  GlobalExceptionHandler.class,
  TraceIdProvider.class,
  TraceIdFilter.class
})
class CostCenterControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean CreateCostCenter create;
  @MockitoBean GetCostCenter get;
  @MockitoBean ListCostCentersByCompany list;
  @MockitoBean DeactivateCostCenter deactivate;

  @Test
  void allEndpointsFollowContract() throws Exception {
    var value = CostCenter.rehydrate(8L, 3L, "Operations", true);
    when(create.execute(3L, "Operations")).thenReturn(value);
    when(get.execute(3L, 8L)).thenReturn(value);
    when(list.execute(eq(3L), any())).thenReturn(new PageResult<>(List.of(value), 0, 20, 1, 1));
    when(deactivate.execute(3L, 8L)).thenReturn(CostCenter.rehydrate(8L, 3L, "Operations", false));
    mvc.perform(
            post("/api/v1/companies/3/cost-centers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Operations\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/companies/3/cost-centers/8"));
    mvc.perform(get("/api/v1/companies/3/cost-centers/8"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.companyId").value(3));
    mvc.perform(get("/api/v1/companies/3/cost-centers?sort=id,desc")).andExpect(status().isOk());
    mvc.perform(post("/api/v1/companies/3/cost-centers/8/deactivate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
  }

  @Test
  void rejectsUnknownAndInvalidSort() throws Exception {
    mvc.perform(
            post("/api/v1/companies/3/cost-centers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\",\"active\":true}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/companies/3/cost-centers?sort=active,asc"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void crossCompanyMissingIsNotFound() throws Exception {
    when(get.execute(2L, 8L)).thenThrow(new CostCenterNotFoundException(2L, 8L));
    mvc.perform(get("/api/v1/companies/2/cost-centers/8"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COST_CENTER_NOT_FOUND"));
  }
}
