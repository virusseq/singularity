package org.cancogenvirusseq.singularity.health;

import lombok.RequiredArgsConstructor;
import org.cancogenvirusseq.singularity.components.pipelines.TotalCountsPipeline;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TotalCountPipelineHealthCheck implements HealthIndicator {

  private static final String MESSAGE_KEY = "totalCountsPipelineDisposable";
  private final TotalCountsPipeline totalCountsPipeline;

  @Override
  public Health health() {
    if (totalCountsPipeline.getPipelineDisposable().isDisposed()) {
      return Health.down().withDetail(MESSAGE_KEY, "disposable has stopped.").build();
    }
    return Health.up().withDetail(MESSAGE_KEY, "disposable is running.").build();
  }
}
