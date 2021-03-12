package org.folio.des.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@RequiredArgsConstructor
public class KafkaConsumerInterceptor implements ConsumerInterceptor<Object, Object> {

  private final FolioModuleMetadata folioModuleMetadata;

  @Override
  public ConsumerRecords<Object, Object> onConsume(ConsumerRecords<Object, Object> records) {
    Iterator<ConsumerRecord<Object, Object>> iterator = records.iterator();
    if (iterator.hasNext()) {
      ConsumerRecord<Object, Object> record = iterator.next();
      Map<String, Collection<String>> okapiHeaders = headersToMap(record.headers());

      var defaultFolioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, okapiHeaders);
      FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(defaultFolioExecutionContext);
    }
    return records;
  }

  @Override
  public void close() {
    // Nothing to do
  }

  @Override
  public void onCommit(Map offsets) {
    FolioExecutionScopeExecutionContextManager.endFolioExecutionContext();
  }

  @Override
  public void configure(Map<String, ?> configs) {
    // Nothing to do
  }

  private Map<String, Collection<String>> headersToMap(Headers header) {
    Iterator<Header> headerIterator = header.iterator();
    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    while (headerIterator.hasNext()) {
      Header next = headerIterator.next();
      if (next.key().startsWith("x-okapi-")) {
        var value = List.of(new String(next.value(), StandardCharsets.UTF_8));
        okapiHeaders.put(next.key(), value);
      }
    }
    return okapiHeaders;
  }

}
