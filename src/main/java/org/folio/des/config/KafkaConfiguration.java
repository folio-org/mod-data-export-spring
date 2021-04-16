package org.folio.des.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnsupportedVersionException;
import org.folio.spring.FolioExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Log4j2
@RequiredArgsConstructor
public class KafkaConfiguration implements DefaultKafkaConsumerFactoryCustomizer {

  @RequiredArgsConstructor
  @Getter
  public enum Topic {
    JOB_COMMAND(".data-export.job.command", Pattern.compile(".+\\.data-export\\.job\\.command$")), JOB_UPDATE(
        ".data-export.job.update", Pattern.compile(".+\\.data-export\\.job\\.update$"));

    private final String nameWithoutTenant;
    private final Pattern pattern;
  }

  private static final int DEFAULT_OPERATION_TIMEOUT = 30;
  private static final String TOPICS_CREATION_ERROR = "Failed to create topics";

  @Value(value = "${spring.kafka.bootstrap-servers}")
  private String boostrapServers;
  @Value(value = "${spring.kafka.listener.ack-mode:batch}")
  private ContainerProperties.AckMode ackMode;

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final FolioExecutionContext folioExecutionContext;
  private final ObjectMapper objectMapper;

  private ConsumerFactory<String, Object> consumerFactory;

  @Override
  @SuppressWarnings("java:S2095")
  // JsonDeserializer is passed to Kafka and will be used there so it can not be closed here as Sonar kindly suggests
  public void customize(DefaultKafkaConsumerFactory<?, ?> consumerFactory) {
    consumerFactory.setValueDeserializer(((JsonDeserializer) new JsonDeserializer<>(objectMapper)).trustedPackages("*"));
    this.consumerFactory = (ConsumerFactory<String, Object>) consumerFactory;
  }

  public void createTopics() {
    String tenant = folioExecutionContext.getTenantId();
    if (StringUtils.isBlank(tenant)) {
      throw new IllegalStateException("Can't create Kafka topics because tenant is blank");
    }
    try (Admin admin = Admin.create(Collections.singletonMap(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, boostrapServers))) {
      addTopicsIfNeeded(admin, Arrays.stream(Topic.values())
          .map(t -> KafkaConfiguration.createTopicObject(tenant + t.getNameWithoutTenant()))
          .collect(Collectors.toList()));
    }
  }

  public void startListener(Topic consumerTopic, AcknowledgingMessageListener<String, ?> listener) {
    ContainerProperties containerProperties = new ContainerProperties(consumerTopic.getPattern());
    containerProperties.setMessageListener(listener);
    containerProperties.setAckMode(ackMode);
    log.info("Creating listener container for topic pattern '{}'.", consumerTopic.getPattern());
    new KafkaMessageListenerContainer<>(consumerFactory, containerProperties).start();
  }

  public void send(Topic topic, String key, Object data) {
    log.info("Sending {}.", data);
    String tenant = folioExecutionContext.getTenantId();
    if (StringUtils.isBlank(tenant)) {
      throw new IllegalStateException("Can't send to Kafka because tenant is blank");
    }
    kafkaTemplate.send(tenant + topic.getNameWithoutTenant(), key, data);
    log.info("Sent {}.", data);
  }

  private static NewTopic createTopicObject(String topic) {
    return TopicBuilder.name(topic).build();
  }

  private void addTopicsIfNeeded(Admin admin, Collection<NewTopic> topics) {
    if (topics.isEmpty()) {
      return;
    }
    Map<String, NewTopic> topicNameToTopic = new HashMap<>();
    topics.forEach(t -> topicNameToTopic.compute(t.name(), (k, v) -> t));
    DescribeTopicsResult topicInfo = admin.describeTopics(topics.stream().map(NewTopic::name).collect(Collectors.toList()));
    List<NewTopic> topicsToAdd = new ArrayList<>();
    Map<String, NewPartitions> topicsToModify = checkPartitions(topicNameToTopic, topicInfo, topicsToAdd);
    if (!topicsToAdd.isEmpty()) {
      addTopics(admin, topicsToAdd);
    }
    if (!topicsToModify.isEmpty()) {
      modifyTopics(admin, topicsToModify);
    }
  }

  private Map<String, NewPartitions> checkPartitions(Map<String, NewTopic> topicNameToTopic, DescribeTopicsResult topicInfo,
      List<NewTopic> topicsToAdd) {
    Map<String, NewPartitions> topicsToModify = new HashMap<>();
    topicInfo.values().forEach((n, f) -> {
      NewTopic topic = topicNameToTopic.get(n);
      try {
        TopicDescription topicDescription = f.get(DEFAULT_OPERATION_TIMEOUT, TimeUnit.SECONDS);
        if (topic.numPartitions() < topicDescription.partitions().size()) {
          log.info(() -> String.format("Topic '%s' exists but has a different partition count: %d not %d", n,
              topicDescription.partitions().size(), topic.numPartitions()));
        } else if (topic.numPartitions() > topicDescription.partitions().size()) {
          log.info(() -> String.format(
              "Topic '%s' exists but has a different partition count: %d not %d, increasing " + "if the broker supports it", n,
              topicDescription.partitions().size(), topic.numPartitions()));
          topicsToModify.put(n, NewPartitions.increaseTo(topic.numPartitions()));
        }
      } catch (@SuppressWarnings("unused") InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (TimeoutException e) {
        throw new KafkaException("Timed out waiting to get existing topics", e);
      } catch (@SuppressWarnings("unused") ExecutionException e) {
        topicsToAdd.add(topic);
      }
    });
    return topicsToModify;
  }

  private void addTopics(Admin admin, List<NewTopic> topicsToAdd) {
    log.info("Creating topic(s) {}.", StringUtils.join(topicsToAdd, ","));
    CreateTopicsResult topicResults = admin.createTopics(topicsToAdd);
    try {
      topicResults.all().get(DEFAULT_OPERATION_TIMEOUT, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Interrupted while waiting for topic creation results", e);
    } catch (TimeoutException e) {
      throw new KafkaException("Timed out waiting for create topics results", e);
    } catch (ExecutionException e) {
      if (e.getCause() instanceof TopicExistsException) { // Possible race with another app instance
        log.debug(TOPICS_CREATION_ERROR, e.getCause());
      } else {
        log.error(TOPICS_CREATION_ERROR, e.getCause());
        throw new KafkaException(TOPICS_CREATION_ERROR, e.getCause()); // NOSONAR
      }
    }
  }

  private void modifyTopics(Admin admin, Map<String, NewPartitions> topicsToModify) {
    log.info("Creating partition(s) {}.", StringUtils.join(topicsToModify.values(), ","));
    CreatePartitionsResult partitionsResult = admin.createPartitions(topicsToModify);
    try {
      partitionsResult.all().get(DEFAULT_OPERATION_TIMEOUT, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Interrupted while waiting for partition creation results", e);
    } catch (TimeoutException e) {
      throw new KafkaException("Timed out waiting for create partitions results", e);
    } catch (ExecutionException e) {
      if (e.getCause() instanceof InvalidPartitionsException) { // Possible race with another app instance
        log.debug(TOPICS_CREATION_ERROR, e.getCause());
      } else {
        log.error("Failed to create partitions", e.getCause());
        if (!(e.getCause() instanceof UnsupportedVersionException)) {
          throw new KafkaException("Failed to create partitions", e.getCause()); // NOSONAR
        }
      }
    }
  }

}
