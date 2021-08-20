package org.cancogenvirusseq.singularity.components.base;

import static org.cancogenvirusseq.singularity.components.model.AnalysisDocument.ID_FIELD;
import static org.cancogenvirusseq.singularity.components.model.AnalysisDocument.LAST_UPDATED_AT_FIELD;

import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.components.model.CountAndLastUpdatedResult;
import org.cancogenvirusseq.singularity.config.elasticsearch.ElasticsearchProperties;
import org.cancogenvirusseq.singularity.config.elasticsearch.ReactiveElasticSearchClientConfig;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.ParsedMax;
import org.elasticsearch.search.aggregations.metrics.ParsedValueCount;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CountAndLastUpdatedAggregation
    implements Function<QueryBuilder, Mono<CountAndLastUpdatedResult>> {
  private final ElasticsearchProperties elasticsearchProperties;
  private final ReactiveElasticSearchClientConfig reactiveElasticSearchClientConfig;

  // aggregation name constants
  private static final String LAST_UPDATED_AGG_NAME = "lastUpdatedDate";
  private static final String TOTAL_HITS_AGG_NAME = "totalHits";

  @Override
  public Mono<CountAndLastUpdatedResult> apply(QueryBuilder queryBuilder) {
    return Mono.just(aggregateSearchSourceBuilderFromQueryBuilder(queryBuilder))
        .map(this::executeAggregationQuery)
        .flatMap(this::extractAggregationResult);
  }

  private SearchSourceBuilder aggregateSearchSourceBuilderFromQueryBuilder(
      QueryBuilder queryBuilder) {
    return new SearchSourceBuilder()
        .query(queryBuilder)
        .aggregation(AggregationBuilders.max(LAST_UPDATED_AGG_NAME).field(LAST_UPDATED_AT_FIELD))
        .aggregation(AggregationBuilders.count(TOTAL_HITS_AGG_NAME).field(ID_FIELD))
        .fetchSource(false);
  }

  private Flux<Aggregation> executeAggregationQuery(SearchSourceBuilder searchSourceBuilder) {
    return reactiveElasticSearchClientConfig
        .reactiveElasticsearchClient()
        .aggregate(
            new SearchRequest()
                .indices(elasticsearchProperties.getFileCentricIndex())
                .source(searchSourceBuilder));
  }

  private Mono<CountAndLastUpdatedResult> extractAggregationResult(
      Flux<Aggregation> aggregationFlux) {
    return aggregationFlux
        .collectMap(Aggregation::getName, aggregation -> aggregation)
        .map(
            aggMap ->
                new CountAndLastUpdatedResult(
                    ((ParsedMax) aggMap.get(LAST_UPDATED_AGG_NAME)),
                    ((ParsedValueCount) aggMap.get(TOTAL_HITS_AGG_NAME))));
  }
}
