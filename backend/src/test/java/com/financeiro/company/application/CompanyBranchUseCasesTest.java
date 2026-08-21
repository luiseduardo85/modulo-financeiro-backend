package com.financeiro.company.application;

import com.financeiro.company.domain.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CompanyBranchUseCasesTest {
    @Test void createsGetsAndListsCompanies() {
        var companies = new Companies();
        Company created = new CreateCompany(companies).execute(" Acme ");
        assertThat(created.id()).isPositive();
        assertThat(created.name()).isEqualTo("Acme");
        assertThat(new GetCompany(companies).execute(created.id())).isEqualTo(created);
        assertThatThrownBy(() -> new GetCompany(companies).execute(999L))
                .isInstanceOf(CompanyNotFoundException.class);

        var query = new PageQuery(0, 20, PageQuery.SortField.ID, PageQuery.SortDirection.ASC);
        PageResult<Company> result = new ListCompanies(companies).execute(query);
        assertThat(result.data()).containsExactly(created);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
    }
    @Test void createsBranchOnlyForExistingCompany() {
        var companies = new Companies(); var branches = new Branches();
        Company company = new CreateCompany(companies).execute(" Acme ");
        Branch branch = new CreateBranch(companies, branches).execute(company.id(), " Main ");
        assertThat(branch.companyId()).isEqualTo(company.id());
        assertThat(branch.name()).isEqualTo("Main");
        assertThatThrownBy(() -> new CreateBranch(companies, branches).execute(999L, "X"))
                .isInstanceOf(CompanyNotFoundException.class);
    }
    @Test void branchLookupIsAlwaysCompanyScoped() {
        var branches = new Branches(); branches.save(Branch.create(1L, "Main"));
        assertThat(new GetBranch(branches).execute(1L, 1L).name()).isEqualTo("Main");
        assertThatThrownBy(() -> new GetBranch(branches).execute(2L, 1L)).isInstanceOf(BranchNotFoundException.class);
        assertThatThrownBy(() -> new GetBranch(branches).execute(1L, 999L)).isInstanceOf(BranchNotFoundException.class);
    }
    @Test void listsOnlyBranchesFromTheExistingCompany() {
        var companies = new Companies(); var branches = new Branches();
        Company one = companies.save(Company.create("One"));
        Company two = companies.save(Company.create("Two"));
        Branch expected = branches.save(Branch.create(one.id(), "Main"));
        branches.save(Branch.create(two.id(), "Foreign"));
        var query = new PageQuery(0, 20, PageQuery.SortField.ID, PageQuery.SortDirection.ASC);
        PageResult<Branch> result = new ListBranchesByCompany(companies, branches).execute(one.id(), query);
        assertThat(result.data()).containsExactly(expected);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
    }
    @Test void listBranchesDistinguishesMissingCompanyFromEmptyPage() {
        var companies = new Companies(); var branches = new Branches();
        Company company = companies.save(Company.create("Acme"));
        var query = new PageQuery(0, 20, PageQuery.SortField.ID, PageQuery.SortDirection.ASC);
        assertThat(new ListBranchesByCompany(companies, branches).execute(company.id(), query).data()).isEmpty();
        assertThatThrownBy(() -> new ListBranchesByCompany(companies, branches).execute(999L, query)).isInstanceOf(CompanyNotFoundException.class);
    }
    @Test void validatesPagination() {
        assertThatThrownBy(() -> new PageQuery(-1, 20, PageQuery.SortField.ID, PageQuery.SortDirection.ASC)).isInstanceOf(InvalidPageRequestException.class);
        assertThatThrownBy(() -> new PageQuery(0, 101, PageQuery.SortField.ID, PageQuery.SortDirection.ASC)).isInstanceOf(InvalidPageRequestException.class);
    }
    static final class Companies implements CompanyRepository {
        final Map<Long,Company> values = new LinkedHashMap<>(); long sequence;
        public Company save(Company c) { var saved=Company.rehydrate(++sequence,c.name()); values.put(sequence,saved); return saved; }
        public Optional<Company> findById(Long id) { return Optional.ofNullable(values.get(id)); }
        public PageResult<Company> findPage(PageQuery q) { return new PageResult<>(values.values().stream().toList(),q.page(),q.size(),values.size(),values.isEmpty()?0:1); }
    }
    static final class Branches implements BranchRepository {
        final Map<Long,Branch> values = new LinkedHashMap<>(); long sequence;
        public Branch save(Branch b) { var saved=Branch.rehydrate(++sequence,b.companyId(),b.name()); values.put(sequence,saved); return saved; }
        public Optional<Branch> findByCompanyIdAndId(Long c,Long id) { return Optional.ofNullable(values.get(id)).filter(b->b.companyId().equals(c)); }
        public PageResult<Branch> findPageByCompanyId(Long c,PageQuery q) { var data=values.values().stream().filter(b->b.companyId().equals(c)).toList(); return new PageResult<>(data,q.page(),q.size(),data.size(),data.isEmpty()?0:1); }
    }
}
