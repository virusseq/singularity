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

package org.cancogenvirusseq.singularity.components;

import static org.cancogenvirusseq.singularity.utils.FileArchiveUtils.downloadPairsToFileArchiveWithInstant;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.components.events.EventEmitter;
import org.cancogenvirusseq.singularity.components.model.AnalysisDocument;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class Files {
  private final EventEmitter eventEmitter;
  private final ElasticQueryToAnalysisDocuments elasticQueryToAnalysisDocuments;
  private final S3Download s3Download;

  @Value("${files.finalEventCheckSeconds}")
  private final Integer finalEventCheckSeconds = 60; // default to 1 minute

  @Value("${files.bundleBuildingCheckSeconds}")
  private final Integer bundleBuildingCheckSeconds = 60; // default to 1 minute

  private static final AtomicReference<Instant> lastEvent = new AtomicReference<>();
  private static final AtomicReference<String> latestFileName = new AtomicReference<>();
  private static final AtomicBoolean isBuildingBundle = new AtomicBoolean(false);

  @Getter private Disposable updateFileBundleDisposable;

  @PostConstruct
  public void init() {
    // set the lastEvent to now (app startup)
    lastEvent.set(Instant.now());

    // build file bundle on app start
    downloadAndBuildBundle(lastEvent.get())
        .doOnNext(latestFileName::set)
        .doFinally(
            signalType -> {
              isBuildingBundle.set(false);
              log.info("Startup file bundle created and saved at: {}", latestFileName.get());
            })
        .subscribe();

    // start file bundle update disposable
    updateFileBundleDisposable = createUpdateFileBundleDisposable();
  }

  public String getFileBundleName() {
    return latestFileName.get();
  }

  /**
   * This disposable subscribes to the eventEmitter flux, be that an interval timer based one or a
   * kafka based one, it records every event received to the static AtomicLong property. After the
   * configured delay it checks to see if the instant recorded is still the same one, meaning there
   * have been no further events, because this is done within a filter, all the intermediary event
   * (ie. those between the first new event inclusive and the last event) are removed, the final
   * event that does make it through will trigger a bundle rebuild.
   *
   * @return disposable of flux that is operating the update mechanism
   */
  private Disposable createUpdateFileBundleDisposable() {
    return eventEmitter
        .receive()
        .doOnNext(
            instant -> {
              log.debug("createUpdateFileBundleDisposable received instant: {}", instant);
              lastEvent.set(instant);
            })
        .transform(takeOnlyFinalInstant)
        .transform(takeOnlyLatestBuildInstant)
        .doOnNext(
            instant -> {
              log.debug(
                  "createUpdateFileBundleDisposable setting isBuildingBundle to true for instant: {}",
                  instant);
            })
        // concat map to guarantee single bundle building at one time
        .concatMap(this::downloadAndBuildBundle)
        .doOnNext(
            archiveFileName -> {
              log.debug(
                  "createUpdateFileBundleDisposable updating latestFileName to: {}",
                  latestFileName);
              latestFileName.set(archiveFileName);
            })
        .log("Files::createUpdateFileBundleDisposable")
        .subscribe();
  }

  private Flux<String> downloadAndBuildBundle(Instant instant) {
    return Mono.just(QueryBuilders.rangeQuery(AnalysisDocument.LAST_UPDATED_AT_FIELD).to(instant))
        .flatMapMany(elasticQueryToAnalysisDocuments)
        .transform(s3Download)
        .transform(downloadPairsToFileArchiveWithInstant(instant))
        // lock bundle building on start of downloadAndSave
        .doOnSubscribe(sub -> isBuildingBundle.set(true))
        // unlock bundle building on complete/error of downloadAndSave
        .doOnTerminate(() -> isBuildingBundle.set(false))
        .log("Files::downloadAndSave");
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

  /**
   * Before kicking off a build we want to ensure a build is not already happening, if it is we want
   * to wait some time (configurable) and then check again, eventually the check passes and we can
   * proceed with the build
   */
  private final Function<Instant, Flux<Instant>> delayForBuildIfBuilding =
      instant ->
          Flux.just(instant)
              .doOnNext(
                  i ->
                      log.debug(
                          "Delay of {} seconds being applied before attempting build for instant: {}",
                          bundleBuildingCheckSeconds,
                          i))
              .delaySequence(Duration.ofSeconds(bundleBuildingCheckSeconds))
              .repeat(
                  () -> {
                    log.debug(
                        "{}: {}",
                        isBuildingBundle.get()
                            ? "Bundle is currently building, will retry for this instant"
                            : "Bundle is not building, proceeding with build for instant",
                        instant);
                    return isBuildingBundle.get();
                  });

  /**
   * Hold on to build requests if a bundle is already building, then once the bundle is built and a
   * new bundle is required, build only the latest one. This ensures that we are not queueing up
   * n..infinity requests while holding to build and instead just build a single bundle, essentially
   * compressing the build requests into a single request
   */
  private final UnaryOperator<Flux<Instant>> takeOnlyLatestBuildInstant =
      buildRequestInstants ->
          buildRequestInstants
              .delayUntil(delayForBuildIfBuilding)
              .filter(
                  instant -> {
                    log.debug(
                        "{}: {}",
                        instant.equals(lastEvent.get())
                            ? "Bundle build is ready to proceed for instant"
                            : "Bundle build for instant is stale, dropping build for instant",
                        instant);
                    return instant.equals(lastEvent.get());
                  });

  private final Consumer<Boolean> setIsBuildingBundle = isBuildingBundle::set;
}
