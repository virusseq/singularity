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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.all.config.elasticsearch.ElasticsearchProperties;
import org.cancogenvirusseq.all.config.elasticsearch.ReactiveElasticSearchClientConfig;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@ConfigurationProperties("files")
public class Files {
  private final ElasticsearchProperties elasticsearchProperties;
  private final ReactiveElasticSearchClientConfig reactiveElasticSearchClientConfig;
  private final Sinks.Many<Instant> sink = Sinks.many().unicast().onBackpressureBuffer();

  @Setter private Integer triggerUpdateDelaySeconds = 60; // default to 1 minute

  @Getter private final AtomicReference<String> latestFileName = new AtomicReference<>();

  @Getter private Disposable updateFileBundleDisposable;

  @PostConstruct
  public void init() {
    updateFileBundleDisposable = createUpdateFileBundleDisposable();
  }

  @Bean
  public Sinks.Many<Instant> fileEventSink() {
    return sink;
  }

  private Disposable createUpdateFileBundleDisposable() {
    return sink.asFlux().doOnNext(event -> log.info(event.toString())).subscribe();
  }

  public Mono<String> getAllFileObjectIds() {
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
        .map(SearchHit::getId)
        .collect(Collectors.joining(","));
  }
}
