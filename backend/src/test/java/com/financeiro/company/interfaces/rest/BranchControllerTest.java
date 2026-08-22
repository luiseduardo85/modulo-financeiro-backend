package com.financeiro.company.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.financeiro.company.application.*;
import com.financeiro.company.domain.Branch;
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

@WebMvcTest(BranchController.class)
@ActiveProfiles("test")
@Import({
  PageQueryParser.class,
  GlobalExceptionHandler.class,
  TraceIdProvider.class,
  TraceIdFilter.class
})
class BranchControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean CreateBranch create;
  @MockitoBean GetBranch get;
  @MockitoBean ListBranchesByCompany list;

  @Test
  void createsFromRouteCompanyOnly() throws Exception {
    when(create.execute(3L, "Main")).thenReturn(Branch.rehydrate(7L, 3L, "Main"));
    mvc.perform(
            post("/api/v1/companies/3/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Main\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/companies/3/branches/7"))
        .andExpect(jsonPath("$.companyId").value(3));
  }

  @Test
  void mapsCrossCompanyAbsenceToStableNotFound() throws Exception {
    when(get.execute(2L, 7L)).thenThrow(new BranchNotFoundException(7L));
    mvc.perform(get("/api/v1/companies/2/branches/7"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("BRANCH_NOT_FOUND"));
  }

  @Test
  void getsBranchSuccessfully() throws Exception {
    when(get.execute(3L, 7L)).thenReturn(Branch.rehydrate(7L, 3L, "Main"));
    mvc.perform(get("/api/v1/companies/3/branches/7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(7))
        .andExpect(jsonPath("$.companyId").value(3))
        .andExpect(jsonPath("$.name").value("Main"));
  }

  @Test
  void listsBranchesSuccessfully() throws Exception {
    when(list.execute(eq(3L), any()))
        .thenReturn(new PageResult<>(List.of(Branch.rehydrate(7L, 3L, "Main")), 0, 20, 1, 1));
    mvc.perform(get("/api/v1/companies/3/branches"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].companyId").value(3))
        .andExpect(jsonPath("$.meta.page").value(0))
        .andExpect(jsonPath("$.meta.size").value(20))
        .andExpect(jsonPath("$.meta.totalElements").value(1))
        .andExpect(jsonPath("$.meta.totalPages").value(1));
  }

  @Test
  void mapsMissingCompanyDuringList() throws Exception {
    when(list.execute(eq(999L), any())).thenThrow(new CompanyNotFoundException(999L));
    mvc.perform(get("/api/v1/companies/999/branches"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
  }

  @Test
  void rejectsCompanyIdInCreationBody() throws Exception {
    mvc.perform(
            post("/api/v1/companies/3/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"companyId\":9,\"name\":\"Main\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    verifyNoInteractions(create);
  }
}
