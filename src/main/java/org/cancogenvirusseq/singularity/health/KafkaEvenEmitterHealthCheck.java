package org.cancogenvirusseq.singularity.health;

import lombok.RequiredArgsConstructor;
import org.cancogenvirusseq.singularity.components.events.TotalCountsKafkaEventEmitter;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("kafka")
@RequiredArgsConstructor
public class KafkaEvenEmitterHealthCheck implements HealthIndicator {
  private static final String MESSAGE_KEY = "kafkaConsumerDisposable";
  private final TotalCountsKafkaEventEmitter kafkaEventEmitter;

  @Override
  public Health health() {
    if (kafkaEventEmitter.getKafkaConsumerDisposable().isDisposed()) {
      return Health.down().withDetail(MESSAGE_KEY, "disposable has stopped.").build();
    }
    return Health.up().withDetail(MESSAGE_KEY, "disposable is running.").build();
  }
}
