ALTER TABLE "financialMovement"
    ADD COLUMN "originalMovementId" BIGINT NULL;

ALTER TABLE "financialMovement"
    ALTER COLUMN "type" TYPE VARCHAR(20);

ALTER TABLE "financialMovement"
    ADD CONSTRAINT "fkFinancialMovementOriginal"
        FOREIGN KEY ("originalMovementId") REFERENCES "financialMovement" ("id");

ALTER TABLE "financialMovement"
    DROP CONSTRAINT "ckFinancialMovementType";

ALTER TABLE "financialMovement"
    ADD CONSTRAINT "ckFinancialMovementType"
        CHECK ("type" IN ('PAYMENT', 'RECEIPT', 'REVERSAL_PAYMENT', 'REVERSAL_RECEIPT'));

ALTER TABLE "financialMovement"
    ADD CONSTRAINT "ckFinancialMovementOriginalConsistency"
        CHECK (
            ("type" IN ('PAYMENT', 'RECEIPT') AND "originalMovementId" IS NULL)
            OR ("type" IN ('REVERSAL_PAYMENT', 'REVERSAL_RECEIPT') AND "originalMovementId" IS NOT NULL)
        );

CREATE INDEX "ixFinancialMovementOriginalMovementId"
    ON "financialMovement" ("originalMovementId")
    WHERE "originalMovementId" IS NOT NULL;
