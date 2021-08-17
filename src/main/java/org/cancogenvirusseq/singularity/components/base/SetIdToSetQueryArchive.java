package org.cancogenvirusseq.singularity.components.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.components.model.ArrangerSetDocument;
import org.cancogenvirusseq.singularity.components.model.SetQueryArchiveHashInfo;
import org.cancogenvirusseq.singularity.config.elasticsearch.ElasticsearchProperties;
import org.cancogenvirusseq.singularity.config.elasticsearch.ReactiveElasticSearchClientConfig;
import org.cancogenvirusseq.singularity.repository.ArchivesRepo;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.TermsLookup;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.ParsedMax;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Function;

import static org.cancogenvirusseq.singularity.components.model.AnalysisDocument.LAST_UPDATED_AT_FIELD;

@Slf4j
@Component
@RequiredArgsConstructor
public class SetIdToSetQueryArchive implements Function<UUID, Mono<Archive>> {
  private final ElasticsearchProperties elasticsearchProperties;
  private final ReactiveElasticSearchClientConfig reactiveElasticSearchClientConfig;
  private final ArchivesRepo archivesRepo;
  private final ObjectMapper objectMapper;

  // aggregation name constants
  private static final String TERMS_LOOKUP_PATH = "ids";
  private static final String LAST_UPDATED_AGG_NAME = "lastUpdatedDate";

  @Override
  public Mono<Archive> apply(UUID uuid) {
    return getArrangerSetDocument(uuid)
        .flatMap(
            arrangerSetDocument ->
                Mono.just(aggregateArrangerSetTermsQuery(uuid))
                    .map(this::executeAggregationQuery)
                    .flatMap(this::extractAggregationResult)
                    .map(
                        lastUpdatedTimestamp ->
                            new SetQueryArchiveHashInfo(
                                arrangerSetDocument.getSqon().toString(), // todo: JSON string?
                                arrangerSetDocument.getSize(),
                                lastUpdatedTimestamp.getValueAsString())))
        .map(Archive::setQueryArchiveFromHashInfo)
        .flatMap(archivesRepo::save); // todo: handle on data integrity violation
  }

  private Mono<ArrangerSetDocument> getArrangerSetDocument(UUID id) {
    return reactiveElasticSearchClientConfig
        .reactiveElasticsearchClient()
        .get(new GetRequest(elasticsearchProperties.getArrangerSetsIndex(), id.toString()))
        .map(this::getResultToArrangerSetDocument);
  }

  @SneakyThrows
  private ArrangerSetDocument getResultToArrangerSetDocument(GetResult getResult) {
    return objectMapper.convertValue(getResult.getSource(), ArrangerSetDocument.class);
  }

  private SearchSourceBuilder aggregateArrangerSetTermsQuery(UUID setId) {
    return new SearchSourceBuilder()
        .query(
            QueryBuilders.termsLookupQuery(
                TERMS_LOOKUP_PATH,
                new TermsLookup(
                    elasticsearchProperties.getArrangerSetsIndex(),
                    setId.toString(),
                    TERMS_LOOKUP_PATH)))
        .aggregation(AggregationBuilders.max(LAST_UPDATED_AGG_NAME).field(LAST_UPDATED_AT_FIELD))
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

  private Mono<ParsedMax> extractAggregationResult(Flux<Aggregation> aggregationFlux) {
    return aggregationFlux
        .collectMap(Aggregation::getName, aggregation -> aggregation)
        .map(aggMap -> ((ParsedMax) aggMap.get(LAST_UPDATED_AGG_NAME)));
  }
}
