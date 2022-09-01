package org.cancogenvirusseq.singularity.config.kafka;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Configuration
@Profile("kafka")
public class KafkaArchiveBuildConsumerConfig {

  @Getter
  private final KafkaReceiver<String, String> receiver;

  public KafkaArchiveBuildConsumerConfig(KafkaProperties properties) {
    receiver = KafkaReceiver.create(buildOptions(properties));
  }

  private ReceiverOptions<String, String> buildOptions(KafkaProperties properties) {
    return ReceiverOptions.<String, String>create(
             Map.ofEntries(
                 new AbstractMap.SimpleEntry<>(
                     ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServer()),
                 new AbstractMap.SimpleEntry<>(
                     ConsumerConfig.CLIENT_ID_CONFIG, properties.getClientId()),
                 new AbstractMap.SimpleEntry<>(
                     ConsumerConfig.GROUP_ID_CONFIG, properties.getGroupId()),
                 new AbstractMap.SimpleEntry<>(
                      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class),
                 new AbstractMap.SimpleEntry<>(
                     ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class),
                 new AbstractMap.SimpleEntry<>(
                     ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, properties.getAutoOffsetReset())))
            .subscription(Collections.singleton(properties.getArchiveBuildTopic()))
            .addAssignListener(partitions -> log.debug("onPartitionsAssigned {}", partitions))
            .addRevokeListener(partitions -> log.debug("onPartitionsRevoked {}", partitions));
  }
}
