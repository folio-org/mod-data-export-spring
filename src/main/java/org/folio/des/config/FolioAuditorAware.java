package org.folio.des.config;

import java.util.Optional;
import java.util.UUID;

import org.folio.spring.FolioExecutionContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import lombok.RequiredArgsConstructor;

/**
 * FolioAuditorAware class is being used for populating "createdBy" field automatically while performing any DB operation.
 * getCurrentAuditor() is needed to get current logged in user.
 */
@Configuration
@EnableJpaAuditing
@RequiredArgsConstructor
public class FolioAuditorAware implements AuditorAware<UUID> {

  private final FolioExecutionContext folioExecutionContext;

  @Override
  public Optional<UUID> getCurrentAuditor() {
    return Optional.ofNullable(folioExecutionContext.getUserId());
  }
}
