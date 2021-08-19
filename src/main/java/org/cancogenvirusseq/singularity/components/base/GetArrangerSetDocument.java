package org.cancogenvirusseq.singularity.components.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.components.model.ArrangerSetDocument;
import org.cancogenvirusseq.singularity.config.elasticsearch.ElasticsearchProperties;
import org.cancogenvirusseq.singularity.config.elasticsearch.ReactiveElasticSearchClientConfig;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.index.get.GetResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetArrangerSetDocument implements Function<UUID, Mono<ArrangerSetDocument>> {
  private final ElasticsearchProperties elasticsearchProperties;
  private final ReactiveElasticSearchClientConfig reactiveElasticSearchClientConfig;
  private final ObjectMapper objectMapper;

  @Override
  public Mono<ArrangerSetDocument> apply(UUID setId) {
    return reactiveElasticSearchClientConfig
        .reactiveElasticsearchClient()
        .get(new GetRequest(elasticsearchProperties.getArrangerSetsIndex(), setId.toString()))
        .map(this::getResultToArrangerSetDocument);
  }

  @SneakyThrows
  private ArrangerSetDocument getResultToArrangerSetDocument(GetResult getResult) {
    return objectMapper.convertValue(getResult.getSource(), ArrangerSetDocument.class);
  }
}
