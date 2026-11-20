package org.folio.des.config.kafka;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.folio.de.entity.Job;
import org.folio.des.domain.dto.JobStatus;
import org.folio.des.repository.JobDataExportRepository;
import org.folio.des.support.BaseTest;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.kafka.filtering.entitlement.TenantEntitlementClient;
import org.folio.spring.kafka.filtering.filter.EnabledTenantMessageFilterStrategy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestPropertySource(properties = {
  "folio.kafka.tenant-filter.enabled=true",
  "folio.kafka.tenant-filter.tenant-disabled-strategy=skip",
  "folio.kafka.tenant-filter.all-tenants-disabled-strategy=skip",
  "spring.kafka.consumer.properties.metadata.max.age.ms=1000"
})
class KafkaTenantFilterEnabledTest extends BaseTest {

  private static final String NOT_ENTITLED_TENANT = "not_entitled";
  private static final String MESSAGE_KEY = "same-key-keeps-messages-in-one-partition";
  private static final long WAIT_MS = 30_000;

  @MockitoBean
  private TenantEntitlementClient tenantEntitlementClient;
  @MockitoBean
  private JobDataExportRepository repository;

  @Autowired
  @Qualifier("tenantAwareMessageFilter")
  private RecordFilterStrategy<?, ?> tenantAwareMessageFilter;
  @Autowired
  private ConcurrentKafkaListenerContainerFactory<String, ?> kafkaListenerContainerFactory;
  @Autowired
  @Qualifier("consumerFactory")
  private ConsumerFactory<String, ?> consumerFactory;
  @Autowired
  private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;
  @Autowired
  private KafkaService kafkaService;
  @Autowired
  private EmbeddedKafkaBroker embeddedKafkaBroker;

  private String jobUpdateTopic;

  @BeforeAll
  void startListenerOnJobUpdateTopic() {
    jobUpdateTopic = kafkaService.getTenantTopicName("data-export.job.update", TENANT);
    embeddedKafkaBroker.addTopics(jobUpdateTopic);
    kafkaService.restartEventListeners();
    waitForPartitionAssignment();
  }

  @BeforeEach
  void stubEntitlements() {
    when(tenantEntitlementClient.lookupTenantsByModuleId(anyString())).thenReturn(Set.of(TENANT));
  }

  @Test
  @DisplayName("Context starts with tenant filter enabled and uses the module consumer factory")
  void contextStartsWithTenantFilterEnabled() {
    assertInstanceOf(EnabledTenantMessageFilterStrategy.class, tenantAwareMessageFilter);
    assertNotNull(kafkaListenerContainerFactory);
    assertSame(consumerFactory, kafkaListenerContainerFactory.getConsumerFactory());
  }

  @Test
  @DisplayName("Update job for entitled tenant")
  void updateJobForEntitledTenant() throws Exception {
    var id = UUID.randomUUID();
    var job = existingJob(id);
    doReturn(Optional.of(job)).when(repository).findById(id);
    doReturn(job).when(repository).save(any());

    sendJobUpdate(TENANT, updatedJob(id));

    verify(repository, timeout(WAIT_MS).times(1)).save(any());
  }

  @Test
  @DisplayName("Skip job update for not entitled tenant")
  void skipJobUpdateForNotEntitledTenant() throws Exception {
    var skippedId = UUID.randomUUID();
    var processedId = UUID.randomUUID();
    var processedJob = existingJob(processedId);
    doReturn(Optional.of(existingJob(skippedId))).when(repository).findById(skippedId);
    doReturn(Optional.of(processedJob)).when(repository).findById(processedId);
    doReturn(processedJob).when(repository).save(any());

    // both messages use the same key, so they are consumed in the order they were sent
    sendJobUpdate(NOT_ENTITLED_TENANT, updatedJob(skippedId));
    sendJobUpdate(TENANT, updatedJob(processedId));

    verify(repository, timeout(WAIT_MS).times(1)).save(any());
    verify(repository, never()).findById(skippedId);
  }

  private static Job existingJob(UUID id) {
    var job = new Job();
    job.setId(id);
    job.setBatchStatus(BatchStatus.COMPLETED);
    job.setStatus(JobStatus.SUCCESSFUL);
    job.setDescription("Test job");
    return job;
  }

  private static Job updatedJob(UUID id) {
    var updatedJob = new Job();
    updatedJob.setId(id);
    updatedJob.setDescription("Test job updated");
    updatedJob.setErrorDetails("Something went wrong");
    return updatedJob;
  }

  private static Map<String, String> okapiHeaders(String tenant) {
    var okapiHeaders = new HashMap<String, String>();
    okapiHeaders.put(XOkapiHeaders.TENANT, tenant);
    okapiHeaders.put(XOkapiHeaders.TOKEN, "TOKEN");
    okapiHeaders.put(XOkapiHeaders.URL, "URL");
    okapiHeaders.put(XOkapiHeaders.USER_ID, UUID.randomUUID().toString());
    return okapiHeaders;
  }

  private void sendJobUpdate(String tenant, Job job) throws Exception {
    var producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
    try (var producer = new KafkaProducer<>(producerProps, new StringSerializer(), new StringSerializer())) {
      var message = new ProducerRecord<>(jobUpdateTopic, MESSAGE_KEY, asJsonString(job));
      okapiHeaders(tenant).forEach((name, value) -> message.headers().add(name, value.getBytes(UTF_8)));
      message.headers().add("__TypeId__", Job.class.getName().getBytes(UTF_8));
      producer.send(message).get();
    }
  }

  private void waitForPartitionAssignment() {
    var container = kafkaListenerEndpointRegistry.getListenerContainer(KafkaService.EVENT_LISTENER_ID);

    assertNotNull(container);
    // the pattern matches only the job update topic created in this test
    ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
  }
}
