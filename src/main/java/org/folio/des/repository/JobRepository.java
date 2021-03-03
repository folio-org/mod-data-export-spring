package org.folio.des.repository;

import org.folio.des.domain.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {
}
