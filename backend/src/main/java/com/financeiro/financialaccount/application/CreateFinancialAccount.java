package com.financeiro.financialaccount.application;

import com.financeiro.category.application.CategoryNotFoundException;
import com.financeiro.category.application.CategoryRepository;
import com.financeiro.company.application.BranchNotFoundException;
import com.financeiro.company.application.BranchRepository;
import com.financeiro.company.application.CompanyNotFoundException;
import com.financeiro.company.application.CompanyRepository;
import com.financeiro.costcenter.application.CostCenterNotFoundException;
import com.financeiro.costcenter.application.CostCenterRepository;
import com.financeiro.financialaccount.domain.FinancialAccount;
import com.financeiro.financialaccount.domain.FinancialAccountType;
import com.financeiro.financialaccount.domain.Installment;
import com.financeiro.partner.application.PartnerNotFoundException;
import com.financeiro.partner.application.PartnerRepository;
import com.financeiro.partner.domain.PartnerRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateFinancialAccount {
  private final CompanyRepository companies;
  private final BranchRepository branches;
  private final PartnerRepository partners;
  private final CategoryRepository categories;
  private final CostCenterRepository costCenters;
  private final FinancialAccountRepository accounts;

  public CreateFinancialAccount(
      CompanyRepository companies,
      BranchRepository branches,
      PartnerRepository partners,
      CategoryRepository categories,
      CostCenterRepository costCenters,
      FinancialAccountRepository accounts) {
    this.companies = companies;
    this.branches = branches;
    this.partners = partners;
    this.categories = categories;
    this.costCenters = costCenters;
    this.accounts = accounts;
  }

  @Transactional
  public FinancialAccount execute(CreateFinancialAccountCommand command) {
    var installments =
        command.installments() == null
            ? null
            : command.installments().stream()
                .map(
                    item ->
                        item == null
                            ? null
                            : Installment.create(
                                item.installmentNumber(), item.dueDate(), item.amount()))
                .toList();
    var candidate =
        FinancialAccount.create(
            command.companyId(),
            command.branchId(),
            command.type(),
            command.partnerId(),
            command.categoryId(),
            command.costCenterId(),
            command.issueDate(),
            command.totalAmount(),
            installments);

    companies
        .findById(candidate.companyId())
        .orElseThrow(() -> new CompanyNotFoundException(candidate.companyId()));
    branches
        .findByCompanyIdAndId(candidate.companyId(), candidate.branchId())
        .orElseThrow(() -> new BranchNotFoundException(candidate.branchId()));
    var partner =
        partners
            .findById(candidate.partnerId())
            .orElseThrow(() -> new PartnerNotFoundException(candidate.partnerId()));
    if (!partner.active()) throw new PartnerInactiveException();
    PartnerRole required =
        candidate.type() == FinancialAccountType.PAYABLE
            ? PartnerRole.SUPPLIER
            : PartnerRole.CUSTOMER;
    if (!partner.roles().contains(required)) throw new PartnerRoleNotAllowedException();
    var category =
        categories
            .findByCompanyIdAndId(candidate.companyId(), candidate.categoryId())
            .orElseThrow(
                () -> new CategoryNotFoundException(candidate.companyId(), candidate.categoryId()));
    if (!category.active()) throw new CategoryInactiveException();
    if (candidate.costCenterId() != null) {
      var costCenter =
          costCenters
              .findByCompanyIdAndId(candidate.companyId(), candidate.costCenterId())
              .orElseThrow(
                  () ->
                      new CostCenterNotFoundException(
                          candidate.companyId(), candidate.costCenterId()));
      if (!costCenter.active()) throw new CostCenterInactiveException();
    }
    return accounts.save(candidate);
  }
}
