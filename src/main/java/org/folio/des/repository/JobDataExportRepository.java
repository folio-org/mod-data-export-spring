package org.folio.des.repository;

import org.folio.de.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface JobDataExportRepository extends JpaRepository<Job, UUID> {

  @Query(value = "SELECT nextval('job-number')", nativeQuery = true)
  Integer getNextJobNumber();

  List<Job> findByUpdatedDateBefore(Date updatedDate);

}
