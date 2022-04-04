package org.cancogenvirusseq.singularity.components.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.components.model.AnalysisDocument;
import org.cancogenvirusseq.singularity.config.elasticsearch.ElasticsearchProperties;
import org.cancogenvirusseq.singularity.config.elasticsearch.ReactiveElasticSearchClientConfig;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticSearchScroll implements Function<QueryBuilder, Flux<AnalysisDocument>> {
  private final ElasticsearchProperties elasticsearchProperties;
  private final ReactiveElasticSearchClientConfig reactiveElasticSearchClientConfig;
  private final ObjectMapper objectMapper;

  @Override
  public Flux<AnalysisDocument> apply(QueryBuilder queryBuilder) {
    return Mono.just(searchSourceBuilderFromQueryBuilder(queryBuilder))
        .flatMapMany(this::executeScrollQuery)
        .map(this::hitMapToAnalysisDocument);
  }

  private SearchSourceBuilder searchSourceBuilderFromQueryBuilder(QueryBuilder queryBuilder) {
    return new SearchSourceBuilder()
        .query(queryBuilder)
        .fetchSource(AnalysisDocument.getEsIncludeFields(), null);
  }

  private Flux<SearchHit> executeScrollQuery(SearchSourceBuilder searchSourceBuilder) {
    return reactiveElasticSearchClientConfig
        .reactiveElasticsearchClient()
        .scroll(
            new SearchRequest()
                .indices(elasticsearchProperties.getFileCentricIndex())
                .source(searchSourceBuilder)
                .scroll(
                    new TimeValue(
                        elasticsearchProperties.getScrollTimeoutMinutes(), TimeUnit.MINUTES)));
  }

  @SneakyThrows
  private AnalysisDocument hitMapToAnalysisDocument(SearchHit hit) {
    return objectMapper.readValue(hit.getSourceAsString(), AnalysisDocument.class);
  }
}
