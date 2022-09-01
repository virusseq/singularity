package org.cancogenvirusseq.singularity.components.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.config.kafka.KafkaArchiveBuildConsumerConfig;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import javax.annotation.PostConstruct;
import java.time.Instant;

@Slf4j
@Component
@Profile("kafka")
@RequiredArgsConstructor
public class AllArchiveBuildKafkaEventEmitter implements EventEmitter<String>  {

  private final KafkaArchiveBuildConsumerConfig kafkaArchiveBuildConsumerConfig;

  private final Sinks.Many<String> proxyManySink = Sinks.many().multicast().onBackpressureBuffer();

  @Getter
  private Disposable kafkaArchiveConsumerDisposable;

  @Override
  public Flux<String> receive() {
    return proxyManySink.asFlux();
  }

  @PostConstruct
  public void init() {
    // setup disposable to events to proxy sink
    kafkaArchiveConsumerDisposable = triggerAllArchiveBuildDisposable();
  }

  private Disposable triggerAllArchiveBuildDisposable(){
    return kafkaArchiveBuildConsumerConfig
            .getReceiver()
            .receiveAutoAck()
            .doOnNext(record -> log.debug("Message received from Kafka cron: {}", record.toString()))
            .map(value -> Instant.now().toString())
            .doOnNext(proxyManySink::tryEmitNext)
            .onErrorContinue(
                    ((throwable, value) ->
                            log.debug("emission {}, threw: {}", throwable, value)))
            .log("AllArchiveBuildKafkaEventEmitter::emit")
            .subscribe();
  }


}
