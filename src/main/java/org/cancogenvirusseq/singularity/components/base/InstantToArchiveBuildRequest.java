package org.cancogenvirusseq.singularity.components.base;

import static org.cancogenvirusseq.singularity.components.model.AnalysisDocument.ID_FIELD;
import static org.cancogenvirusseq.singularity.components.model.AnalysisDocument.LAST_UPDATED_AT_FIELD;

import java.time.Instant;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.components.model.ArchiveBuildRequest;
import org.cancogenvirusseq.singularity.config.elasticsearch.ElasticsearchProperties;
import org.cancogenvirusseq.singularity.config.elasticsearch.ReactiveElasticSearchClientConfig;
import org.cancogenvirusseq.singularity.repository.ArchivesRepo;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.repository.model.ArchiveStatus;
import org.cancogenvirusseq.singularity.repository.model.ArchiveType;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.ParsedMax;
import org.elasticsearch.search.aggregations.metrics.ParsedValueCount;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstantToArchiveBuildRequest implements Function<Instant, Mono<ArchiveBuildRequest>> {
  private final ElasticsearchProperties elasticsearchProperties;
  private final ReactiveElasticSearchClientConfig reactiveElasticSearchClientConfig;
  private final ArchivesRepo archivesRepo;

  // aggregation name constants
  private static final String LAST_UPDATED_AGG_NAME = "lastUpdatedDate";
  private static final String TOTAL_HITS_AGG_NAME = "totalHits";

  @Override
  public Mono<ArchiveBuildRequest> apply(Instant instant) {
    return Mono.just(aggregateSearchSourceBuilderFromInstant(instant))
        .map(this::executeAggregationQuery)
        .flatMap(this::extractAggregationResultsToTuple)
        .flatMap(this::createAndSaveArchiveToDatabase)
        // why this? because R2DBC does not hydrate fields
        // (https://github.com/spring-projects/spring-data-r2dbc/issues/455)
        .flatMap(archivesRepo::findByArchiveObject)
        .map(transformToArchiveBuildRequest(instant))
        .onErrorStop()
        .log("InstantToArchiveBuildRequest");
  }

  private SearchSourceBuilder aggregateSearchSourceBuilderFromInstant(Instant instant) {
    return new SearchSourceBuilder()
        .query(QueryBuilders.rangeQuery(LAST_UPDATED_AT_FIELD).to(instant))
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

  private Mono<Tuple2<ParsedMax, ParsedValueCount>> extractAggregationResultsToTuple(
      Flux<Aggregation> aggregationFlux) {
    return aggregationFlux
        .collectMap(Aggregation::getName, aggregation -> aggregation)
        .map(
            aggMap ->
                Tuples.of(
                    ((ParsedMax) aggMap.get(LAST_UPDATED_AGG_NAME)),
                    ((ParsedValueCount) aggMap.get(TOTAL_HITS_AGG_NAME))));
  }

  private Mono<Archive> createAndSaveArchiveToDatabase(
      Tuple2<ParsedMax, ParsedValueCount> aggTuple) {
    return archivesRepo.save(
        Archive.builder()
            .status(ArchiveStatus.BUILDING)
            .type(ArchiveType.ALL)
            .hashInfo(aggTuple.getT1().getValueAsString())
            .numOfSamples((int) aggTuple.getT2().getValue())
            .build());
  }

  private Function<Archive, ArchiveBuildRequest> transformToArchiveBuildRequest(Instant instant) {
    return archive ->
        new ArchiveBuildRequest(
            archive, QueryBuilders.rangeQuery(LAST_UPDATED_AT_FIELD).to(instant), instant);
  }
}
