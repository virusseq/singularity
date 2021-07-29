package org.cancogenvirusseq.singularity.components;

import java.time.Instant;
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
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.ParsedMax;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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
                .aggregation(AggregationBuilders.max(AnalysisDocument.LAST_UPDATED_AT_FIELD))
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
                aggregationFlux.map(aggregation -> ((ParsedMax) aggregation).getValue()).next())
        .flatMap(
            latestUpdatedDate ->
                archivesRepo.save(
                    Archive.builder()
                        .status(ArchiveStatus.BUILDING)
                        .type(ArchiveType.ALL)
                        .hashInfo(latestUpdatedDate.toString())
                        .numOfSamples(0) // todo make this real
                        .build()))
        .map(
            archive ->
                new ArchiveBuildRequest(
                    archive,
                    QueryBuilders.rangeQuery(AnalysisDocument.LAST_UPDATED_AT_FIELD).to(instant)))
        .onErrorStop()
        .log("InstantToArchiveBuildRequest");
  }
}
