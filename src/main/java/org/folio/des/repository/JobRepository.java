package org.folio.des.repository;

import org.folio.des.domain.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

  @Query(value = "SELECT nextval('job-number')", nativeQuery = true)
  Integer getNextJobNumber();

}
