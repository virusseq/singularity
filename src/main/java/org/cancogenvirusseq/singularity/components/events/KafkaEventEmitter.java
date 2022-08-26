/*
 * Copyright (c) 2021 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.cancogenvirusseq.singularity.components.events;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.config.kafka.KafkaConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Slf4j
@Component
@Profile("kafka")
@RequiredArgsConstructor
public class KafkaEventEmitter implements EventEmitter<Instant> {
  private final KafkaConsumerConfig kafkaConsumerConfig;

  @Value("${files.finalEventCheckSeconds}")
  private final Integer finalEventCheckSeconds = 60; // default to 1 minute

  private static final AtomicReference<Instant> lastEvent = new AtomicReference<>();

  private final Sinks.Many<Instant> proxyManySink = Sinks.many().multicast().onBackpressureBuffer();

  @Getter private Disposable kafkaConsumerDisposable;

  @PostConstruct
  public void init() {
    // setup disposable to events to proxy sink
    kafkaConsumerDisposable = createKafkaConsumeAndSinkDisposable();
    System.out.println("INIT CALLED in KafkaEventEmitter");
  }

  @Override
  public Flux<Instant> receive() {
    // reactor kafka doesn't allow multiple subscriptions on its fluxes.
    // We only use kafa messages as an event source to trigger instants.
    // The proxyManySink is used to tryEmit the instants to trigger
    // operations for multiple subscribed components
    return proxyManySink.asFlux();
  }

  private Disposable createKafkaConsumeAndSinkDisposable() {
    return kafkaConsumerConfig
        .getReceiver()
        .receiveAutoAck()
        .concatMap(r -> r)
        .doOnNext(record -> log.debug("Message received from Kafka: {}", record.toString()))
        // we dont' actually care about the message contents so we just emit and Instant here
        // instead
        .map(value -> Instant.now())
        .doOnNext(lastEvent::set)
        .transform(takeOnlyFinalInstant)
        .doOnNext(proxyManySink::tryEmitNext)
        .onErrorContinue(
            ((throwable, value) ->
                log.debug("intervalEmit emission {}, threw: {}", throwable, value)))
        .log("KafkaEventEmitter::emit")
        .subscribe();
  }

  /**
   * As events come in, we only want to trigger the bundle building at the tail end of a submission,
   * meaning that we do not want to start building the bundle until every event for a particular
   * submission has been received thereby letting us know that we can start building, we accomplish
   * this by delaying all elements by some set time and then filtering all events that are not the
   * most recent event. Pseudo Example:
   *
   * <p>emit a sequential number every 10 second ... 4 3 2 1 -> setLatest(x) -> wait 15 seconds ->
   * current: 1, latest: 2 (filter fail) ... current: 4, latest: 4 (filter pass) -< request build
   */
  private final UnaryOperator<Flux<Instant>> takeOnlyFinalInstant =
      events ->
          events
              .delaySequence(Duration.ofSeconds(finalEventCheckSeconds))
              .filter(
                  instant -> {
                    log.debug(
                        "Current instant: {}, lastEvent: {}, {}",
                        instant,
                        lastEvent.get(),
                        instant.equals(lastEvent.get()) ? "does match" : "does not match");
                    return instant.equals(lastEvent.get());
                  });
}
