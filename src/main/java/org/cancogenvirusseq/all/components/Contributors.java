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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.cancogenvirusseq.all.config.elasticsearch.ElasticsearchProperties;
import org.cancogenvirusseq.all.config.elasticsearch.ReactiveElasticSearchClientConfig;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@ConfigurationProperties("contributors")
public class Contributors {
  private final ElasticsearchProperties elasticsearchProperties;
  private final ReactiveElasticSearchClientConfig reactiveElasticSearchClientConfig;

  // Config values
  @Setter private String[] filterList = new String[] {};
  @Setter private String[] appendList = new String[] {};

  private static final Integer MAX_AGGREGATE_BUCKETS = 1000;

  public Mono<Set<String>> getContributors() {
    return Mono.just(
            new SearchSourceBuilder()
                .aggregation(
                    AggregationBuilders.terms("collectors")
                        .field("analysis.sample_collection.sample_collected_by")
                        .size(MAX_AGGREGATE_BUCKETS))
                .aggregation(
                    AggregationBuilders.terms("submitters")
                        .field("analysis.sample_collection.sequence_submitted_by")
                        .size(MAX_AGGREGATE_BUCKETS))
                .size(0)) // number of documents returned by query set to zero, buckets set above
        .map(
            source ->
                reactiveElasticSearchClientConfig
                    .reactiveElasticsearchClient()
                    .aggregate(
                        new SearchRequest()
                            .indices(elasticsearchProperties.getFileCentricIndex())
                            .source(source)))
        .flatMapMany(
            aggregationFlux ->
                aggregationFlux.map(aggregation -> ((ParsedStringTerms) aggregation).getBuckets()))
        .flatMapIterable(
            buckets ->
                buckets.stream()
                    .map(bucket -> bucket.getKey().toString())
                    .collect(Collectors.toSet()))
        .filter(
            contributor ->
                Arrays.stream(filterList).noneMatch(filter -> filter.equals(contributor)))
        .concatWith(Flux.fromStream(Arrays.stream(appendList)))
        .collect(Collectors.toSet());
  }
}
