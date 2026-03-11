package org.folio.des.client;

import org.folio.des.domain.dto.PresignedUrl;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("refresh-presigned-url")
public interface ExportWorkerClient {

  @GetExchange
  PresignedUrl getRefreshedPresignedUrl(@RequestParam("filePath") String filePath);
}
