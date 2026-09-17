package org.folio.des.support;

import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.awaitility.Awaitility.await;

import java.io.Closeable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;

/**
 * Reusable test consumer that subscribes to a single Kafka topic with a raw {@code String} value deserializer and
 * buffers every received record. Integration tests use it to assert on the JSON payload of published domain events.
 *
 * <p>Adapted from the mod-notes {@code TestKafkaConsumer} pattern. Callers
 * {@link #subscribe(String, KafkaProperties)} it, {@link #poll(String)} the buffered records filtered by key, and
 * {@link #close()} it when done (it is {@link Closeable}, so it works with try-with-resources).</p>
 */
public final class TestKafkaConsumer implements Closeable {

  private static final Duration DEFAULT_POLL_TIMEOUT = Duration.ofMinutes(1);
  private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

  private final KafkaMessageListenerContainer<String, String> container;
  private final BlockingQueue<ConsumerRecord<String, String>> records = new LinkedBlockingQueue<>();
  private final List<ConsumerRecord<String, String>> buffer = new ArrayList<>();

  private TestKafkaConsumer(KafkaMessageListenerContainer<String, String> container) {
    this.container = container;
  }

  /**
   * Creates and starts a consumer subscribed to the given topic.
   *
   * @param topic      the topic to consume from (already env/tenant qualified)
   * @param properties Spring Kafka properties (bootstrap servers point at the embedded broker)
   * @return a started consumer; close it when done
   */
  public static TestKafkaConsumer subscribe(String topic, KafkaProperties properties) {
    createTopic(topic, properties);
    // Override consumer settings on a local copy only — never mutate the shared Spring KafkaProperties bean,
    // otherwise concurrently-running tests inherit this consumer's group id / offset reset.
    Map<String, Object> config = new HashMap<>(properties.buildConsumerProperties());
    config.put(GROUP_ID_CONFIG, "mod-data-export-spring-test-group-" + UUID.randomUUID());
    config.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
    config.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    config.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

    var consumerFactory = new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), new StringDeserializer());
    var containerProperties = new ContainerProperties(topic);
    var container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);

    var consumer = new TestKafkaConsumer(container);
    container.setupMessageListener((MessageListener<String, String>) consumer.records::add);
    container.start();
    return consumer;
  }

  /**
   * Eagerly creates the topic via an admin client so the producer does not race the broker's lazy auto-creation
   * (which otherwise surfaces as {@code Topic ... not present in metadata} on the first send).
   *
   * @param topic      the topic to create (no-op if it already exists)
   * @param properties Spring Kafka properties (used for the bootstrap servers)
   */
  private static void createTopic(String topic, KafkaProperties properties) {
    try (var admin = Admin.create(properties.buildAdminProperties())) {
      admin.createTopics(List.of(new NewTopic(topic, 1, (short) 1))).all().get();
    } catch (ExecutionException e) {
      if (!(e.getCause() instanceof TopicExistsException)) {
        throw new IllegalStateException("Failed to create test topic " + topic, e);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while creating test topic " + topic, e);
    }
  }

  /**
   * Waits (up to one minute) until at least one record with the given key has been received and returns all such
   * records, failing the calling test if none arrive.
   *
   * @param key the record key to filter on (the config id)
   * @return the matching records
   */
  public List<ConsumerRecord<String, String>> poll(String key) {
    var matched = new ArrayList<ConsumerRecord<String, String>>();
    await().pollInterval(POLL_INTERVAL).atMost(DEFAULT_POLL_TIMEOUT)
      .untilAsserted(() -> {
        records.drainTo(buffer);
        var found = buffer.stream()
          .filter(e -> Objects.equals(e.key(), key))
          .toList();
        if (found.isEmpty()) {
          throw new AssertionError("No record received yet for key " + key);
        }
        matched.clear();
        matched.addAll(found);
      });
    return matched;
  }

  /**
   * Drains records for up to the given duration and returns every buffered record matching the key (possibly
   * empty). Unlike {@link #poll(String)} this never fails on an empty result, so it can assert that <em>no</em>
   * event was published. It blocks on the record queue rather than sleeping a fixed interval.
   *
   * @param key      the record key to filter on (the config id)
   * @param duration the maximum time to wait for records to arrive
   * @return the matching records seen within the window (empty if none arrived)
   */
  public List<ConsumerRecord<String, String>> drainFor(String key, Duration duration) {
    long deadline = System.nanoTime() + duration.toNanos();
    long remaining;
    try {
      while ((remaining = deadline - System.nanoTime()) > 0) {
        var polled = records.poll(remaining, TimeUnit.NANOSECONDS);
        if (polled != null) {
          buffer.add(polled);
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while draining test topic", e);
    }
    return buffer.stream()
      .filter(e -> Objects.equals(e.key(), key))
      .toList();
  }

  @Override
  public void close() {
    container.stop();
  }
}




