package com.financeiro.approval.infrastructure;

import com.financeiro.approval.application.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApprovalBoundaryConfiguration {
  @Bean
  @ConditionalOnMissingBean(ApprovalActorContext.class)
  ApprovalActorContext unavailableApprovalActorContext() {
    return () -> {
      throw new ApprovalActorUnavailableException();
    };
  }

  @Bean
  @ConditionalOnMissingBean(ApprovalEligibility.class)
  ApprovalEligibility denyApprovalEligibilityUntilAuthorizationIsIntegrated() {
    return (actorId, companyId) -> false;
  }
}
