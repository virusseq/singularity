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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.components.events.EventEmitter;
import org.cancogenvirusseq.singularity.components.model.AnalysisDocument;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;

import static org.cancogenvirusseq.singularity.utils.FileArchiveUtils.deleteArchive;
import static org.cancogenvirusseq.singularity.utils.FileArchiveUtils.downloadPairsToFileArchiveWithInstant;

@Slf4j
@Component
@RequiredArgsConstructor
public class Files {
  private final EventEmitter eventEmitter;
  private final ElasticQueryToAnalysisDocuments elasticQueryToAnalysisDocuments;
  private final S3Download s3Download;

  @Getter private Disposable allArchiveDisposable;
  @Getter private Disposable buildAllArchiveDisposable;

  @PostConstruct
  public void init() {
    // build file archive on app start
    buildAllArchiveDisposable = createBuildAllArchiveDisposable(Instant.now());

    // start file bundle update disposable
    allArchiveDisposable = createAllArchiveDisposable();
  }

  private Disposable createAllArchiveDisposable() {
    return eventEmitter
        .receive()
        .doOnNext(
            instant -> {
              log.debug("createAllArchiveDisposable received instant: {}", instant);

              if (!buildAllArchiveDisposable.isDisposed()) {
                log.debug("Existing archive build detected!");
                this.buildAllArchiveDisposable.dispose();
              }

              log.debug("Spawning new archive build...");
              this.buildAllArchiveDisposable = createBuildAllArchiveDisposable(instant);
            })
        .log("Files::createAllArchiveDisposable")
        .subscribe();
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
  private Disposable createBuildAllArchiveDisposable(Instant instant) {
    return Mono.just(QueryBuilders.rangeQuery(AnalysisDocument.LAST_UPDATED_AT_FIELD).to(instant))
        // concat map to guarantee single bundle building at one time
        .flatMapMany(elasticQueryToAnalysisDocuments)
        .delaySequence(
            Duration.ofSeconds(
                5)) // temp ... TODO: will want a flux context with the right stuff to execute a
                    // delete on cancel or complete + do the DB operations
        .transform(s3Download)
        .transform(downloadPairsToFileArchiveWithInstant(instant))
        .doOnNext(deleteArchive)
        .doOnComplete(() -> log.debug("createBuildAllArchiveDisposable is done!"))
        .log("Files::createBuildAllArchiveDisposable")
        .subscribe();
  }
}
