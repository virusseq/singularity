package org.cancogenvirusseq.singularity.components.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.components.model.ArchiveBuildRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveBuildRequestEmitter implements EventEmitter<ArchiveBuildRequest> {

  @Getter
  private final Sinks.Many<ArchiveBuildRequest> sink =
      Sinks.many().unicast().onBackpressureBuffer();

  @Override
  public Flux<ArchiveBuildRequest> receive() {
    return sink.asFlux();
  }
}
