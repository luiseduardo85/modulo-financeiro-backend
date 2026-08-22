package com.financeiro.cashflow.infrastructure.persistence;

import com.financeiro.cashflow.application.CashFlowAggregateRow;
import com.financeiro.cashflow.application.CashFlowQuery;
import com.financeiro.cashflow.application.CashFlowRepository;
import com.financeiro.financialmovement.infrastructure.persistence.NetSettlementAmountSql;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public class JpaCashFlowRepositoryAdapter implements CashFlowRepository {
  private static final String FORECAST_QUERY_TEMPLATE =
      """
      WITH "remaining" AS (
          SELECT i."dueDate" AS "dueDate",
                 fa."type" AS "accountType",
                 i."amount" - COALESCE((
                     SELECT SUM(%s)
                     FROM "financialMovement" mv
                     WHERE mv."installmentId" = i."id"
                 ), 0) AS "remainingAmount"
          FROM "installment" i
          JOIN "financialAccount" fa ON fa."id" = i."financialAccountId"
          WHERE fa."companyId" = :companyId
            AND fa."status" = 'APPROVED'
            AND i."dueDate" BETWEEN :startDate AND :endDate
            %s
      )
      SELECT "dueDate",
             SUM(CASE WHEN "accountType" = 'RECEIVABLE' THEN "remainingAmount" ELSE 0 END) AS "inflow",
             SUM(CASE WHEN "accountType" = 'PAYABLE' THEN "remainingAmount" ELSE 0 END) AS "outflow"
      FROM "remaining"
      WHERE "remainingAmount" > 0
      GROUP BY "dueDate"
      ORDER BY "dueDate"
      """;

  private static final String ACTUAL_QUERY_TEMPLATE =
      """
      SELECT mv."movementDate" AS "movementDate",
             SUM(CASE WHEN mv."type" IN ('RECEIPT', 'REVERSAL_PAYMENT') THEN mv."amount" ELSE 0 END) AS "inflow",
             SUM(CASE WHEN mv."type" IN ('PAYMENT', 'REVERSAL_RECEIPT') THEN mv."amount" ELSE 0 END) AS "outflow"
      FROM "financialMovement" mv
      JOIN "installment" i ON i."id" = mv."installmentId"
      JOIN "financialAccount" fa ON fa."id" = i."financialAccountId"
      WHERE fa."companyId" = :companyId
        AND mv."movementDate" BETWEEN :startDate AND :endDate
        %s
      GROUP BY mv."movementDate"
      ORDER BY mv."movementDate"
      """;

  private static final String FORECAST_QUERY =
      FORECAST_QUERY_TEMPLATE.formatted(NetSettlementAmountSql.NET_AMOUNT_CASE, "%s");

  private static final String BRANCH_FILTER = "AND fa.\"branchId\" = :branchId";

  @PersistenceContext private EntityManager entityManager;

  @Override
  @SuppressWarnings("unchecked")
  public List<CashFlowAggregateRow> forecastByCompany(CashFlowQuery query) {
    return toRows((List<Object[]>) buildQuery(FORECAST_QUERY, query).getResultList());
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<CashFlowAggregateRow> actualByCompany(CashFlowQuery query) {
    return toRows((List<Object[]>) buildQuery(ACTUAL_QUERY_TEMPLATE, query).getResultList());
  }

  private Query buildQuery(String template, CashFlowQuery query) {
    String sql = template.formatted(query.branchId() != null ? BRANCH_FILTER : "");
    Query nativeQuery =
        entityManager
            .createNativeQuery(sql)
            .setParameter("companyId", query.companyId())
            .setParameter("startDate", query.startDate())
            .setParameter("endDate", query.endDate());
    if (query.branchId() != null) nativeQuery.setParameter("branchId", query.branchId());
    return nativeQuery;
  }

  private static List<CashFlowAggregateRow> toRows(List<Object[]> rows) {
    List<CashFlowAggregateRow> result = new ArrayList<>();
    for (Object[] r : rows) {
      result.add(new CashFlowAggregateRow(toLocalDate(r[0]), (BigDecimal) r[1], (BigDecimal) r[2]));
    }
    return result;
  }

  private static LocalDate toLocalDate(Object value) {
    if (value instanceof LocalDate localDate) return localDate;
    if (value instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
    throw new IllegalStateException("Unsupported date type: " + value.getClass());
  }
}
