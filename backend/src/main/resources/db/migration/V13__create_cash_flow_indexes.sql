CREATE INDEX "ixInstallmentDueDate" ON "installment" ("dueDate");

CREATE INDEX "ixFinancialAccountCompanyBranchStatus"
    ON "financialAccount" ("companyId", "branchId", "status");

CREATE INDEX "ixFinancialMovementMovementDate" ON "financialMovement" ("movementDate");
