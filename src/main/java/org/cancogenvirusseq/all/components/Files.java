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

package org.cancogenvirusseq.all.components;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.all.components.events.EventEmitter;
import org.cancogenvirusseq.all.config.elasticsearch.ElasticsearchProperties;
import org.cancogenvirusseq.all.config.elasticsearch.ReactiveElasticSearchClientConfig;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class Files {
  private final ElasticsearchProperties elasticsearchProperties;
  private final ReactiveElasticSearchClientConfig reactiveElasticSearchClientConfig;
  private final EventEmitter eventEmitter;
  private final Download download;

  @Value("${files.triggerUpdateDelaySeconds}")
  private final Integer triggerUpdateDelaySeconds = 60; // default to 1 minute

  private static final AtomicReference<Instant> lastEvent = new AtomicReference<>();
  private static final AtomicReference<String> latestFileName = new AtomicReference<>();

  @Getter private Disposable updateFileBundleDisposable;

  @PostConstruct
  public void init() {
    updateFileBundleDisposable = createUpdateFileBundleDisposable();

    // build a bundle on app start
    downloadAndSave(Instant.now())
        .doOnNext(latestFileName::set)
        .doFinally(
            signalType ->
                log.info("Startup file bundle created and saved at: {}", latestFileName.get()))
        .blockFirst();
  }

  // TODO this will return the file zip
  public String getFileBundle() {
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
        .doOnNext(lastEvent::set)
        .delayElements(Duration.ofSeconds(triggerUpdateDelaySeconds))
        .filter(instant -> instant.equals(lastEvent.get()))
        .flatMap(this::downloadAndSave)
        .doOnNext(latestFileName::set)
        .doOnNext(log::info)
        .subscribe();
  }

  private Flux<String> downloadAndSave(Instant instant) {
    return getAllFileObjectIds().transform(download.makeDownloadGzipFunction(instant));
  }

  private Flux<String> getAllFileObjectIds() {
    return Mono.just(
            new SearchSourceBuilder()
                .query(QueryBuilders.matchAllQuery())
                .sort(new FieldSortBuilder("analysis.updated_at").order(SortOrder.DESC))
                .fetchSource(false))
        .flatMapMany(
            source ->
                reactiveElasticSearchClientConfig
                    .reactiveElasticsearchClient()
                    .scroll(
                        new SearchRequest()
                            .indices(elasticsearchProperties.getFileCentricIndex())
                            .source(source)))
        .map(hit -> hit.getId());
  }
}
