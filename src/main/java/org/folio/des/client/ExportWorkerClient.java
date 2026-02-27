package org.folio.des.client;

import org.folio.des.domain.dto.PresignedUrl;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("refresh-presigned-url")
public interface ExportWorkerClient {

  @GetMapping
  PresignedUrl getRefreshedPresignedUrl(@RequestParam("filePath") String filePath);
}
