package org.cancogenvirusseq.singularity.components;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.components.model.AnalysisDocument;
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
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstantToArchiveBuildRequest implements Function<Instant, Mono<ArchiveBuildRequest>> {
  private final ElasticsearchProperties elasticsearchProperties;
  private final ReactiveElasticSearchClientConfig reactiveElasticSearchClientConfig;
  private final ArchivesRepo archivesRepo;

  @Override
  public Mono<ArchiveBuildRequest> apply(Instant instant) {
    return Mono.just(
            new SearchSourceBuilder()
                .query(QueryBuilders.rangeQuery(AnalysisDocument.LAST_UPDATED_AT_FIELD).to(instant))
                .aggregation(
                    AggregationBuilders.max("lastUpdatedDate")
                        .field(AnalysisDocument.LAST_UPDATED_AT_FIELD))
                .aggregation(AggregationBuilders.count("totalHits").field("_id"))
                .fetchSource(false))
        .map(
            source ->
                reactiveElasticSearchClientConfig
                    .reactiveElasticsearchClient()
                    .aggregate(
                        new SearchRequest()
                            .indices(elasticsearchProperties.getFileCentricIndex())
                            .source(source)))
        .flatMap(
            aggregationFlux ->
                aggregationFlux
                    .collectMap(Aggregation::getName, aggregation -> aggregation)
                    .map(
                        aggMap ->
                            Tuples.of(
                                ((ParsedMax) aggMap.get("lastUpdatedDate")),
                                ((ParsedValueCount) aggMap.get("totalHits")))))
        .flatMap(
            aggTuple ->
                archivesRepo.save(
                    Archive.builder()
                        .status(ArchiveStatus.BUILDING)
                        .type(ArchiveType.ALL)
                        .hashInfo(aggTuple.getT1().getValueAsString())
                        .numOfSamples((int) aggTuple.getT2().getValue())
                        .build()))
        // why this? because R2DBC does not hydrate fields
        // (https://github.com/spring-projects/spring-data-r2dbc/issues/455)
        .flatMap(archivesRepo::findByArchiveObject)
        .map(
            archive ->
                new ArchiveBuildRequest(
                    archive,
                    QueryBuilders.rangeQuery(AnalysisDocument.LAST_UPDATED_AT_FIELD).to(instant),
                    instant))
        .onErrorStop()
        .log("InstantToArchiveBuildRequest");
  }
}
