package org.cancogenvirusseq.singularity.components.events;

import java.util.UUID;
import lombok.Getter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class ArrangerSetIdEmitter implements EventEmitter<UUID> {

  @Getter private final Sinks.Many<UUID> sink = Sinks.many().unicast().onBackpressureBuffer();

  @Override
  public Flux<UUID> receive() {
    return sink.asFlux();
  }
}
