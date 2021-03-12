package org.folio.des.config;

import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

@Component
@NoArgsConstructor
@Log4j2
public class KafkaProducerInterceptor implements ProducerInterceptor<Object, Object> {

  @Override
  public ProducerRecord<Object, Object> onSend(ProducerRecord<Object, Object> record) {
    FolioExecutionContext folioExecutionContext = fetchContext();

    if (folioExecutionContext != null) {
      Map<String, Collection<String>> okapiHeaders = folioExecutionContext.getOkapiHeaders();
      okapiHeaders.entrySet().stream().map(toRecordHeader()).forEach(recordHeader -> record.headers().add(recordHeader));
    }

    return record;
  }

  @Override
  public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
    // Nothing to do
  }

  @Override
  public void close() {
    // Nothing to do
  }

  @Override
  public void configure(Map<String, ?> configs) {
    // Nothing to do
  }

  private FolioExecutionContext fetchContext() {
    Method method;
    try {
      method = FolioExecutionScopeExecutionContextManager.class.getDeclaredMethod("getFolioExecutionContext");
      method.setAccessible(true);
      return (FolioExecutionContext) method.invoke(null);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      log.error("Can not fetch FolioExecutionContext cause {}", e.getMessage());
    }
    return null;
  }

  private Function<Entry<String, Collection<String>>, RecordHeader> toRecordHeader() {
    return entry -> {
      byte[] value = entry.getValue().stream().findFirst().orElse("").getBytes(StandardCharsets.UTF_8);
      return new RecordHeader(entry.getKey(), value);
    };
  }

}
