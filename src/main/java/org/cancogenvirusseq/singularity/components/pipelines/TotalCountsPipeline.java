package org.cancogenvirusseq.singularity.components.pipelines;

import static org.cancogenvirusseq.singularity.components.model.AnalysisDocument.LAST_UPDATED_AT_FIELD;
import static org.cancogenvirusseq.singularity.components.utils.ConverterUtils.convertBytesToHumanReadable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.singularity.components.events.EventEmitter;
import org.cancogenvirusseq.singularity.components.model.TotalCounts;
import org.cancogenvirusseq.singularity.config.elasticsearch.ElasticsearchProperties;
import org.cancogenvirusseq.singularity.repository.TotalCountsRepo;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.metrics.ParsedStats;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class TotalCountsPipeline {
  private static final Integer MAX_AGGREGATE_BUCKETS = 1000;
  private static final Integer SIZE = 10000;
  private static final String FIELD_DONORS = "donors";
  private static final String FIELD_SUBMITTER_DONOR_ID = "submitter_donor_id";
  private static final String FIELD_STUDY_ID = "study_id";
  private static final String FIELD_FILE_SIZE = "file.size";

  private static final String[] ES_INCLUDE = {FIELD_DONORS + "." + FIELD_SUBMITTER_DONOR_ID};

  private final ElasticsearchProperties properties;
  private final ReactiveElasticsearchClient client;
  private final EventEmitter<Instant> eventEmitter;

  private final TotalCountsRepo totalCountsRepo;

  @Getter private Disposable pipelineDisposable;
  private Disposable calculatorDisposable;

  @PostConstruct
  public void init() {
    log.info("CachedTotalCounts is empty - Calculating");

    // create calculator disposable on start up to fetch and count things
    calculatorDisposable = createCalculatorDisposable(Instant.now());

    // totalCountsDisposable will update total count on events received
    pipelineDisposable = createTotalCountsPipelineDisposable();
  }

  public Mono<TotalCounts> getTotalCounts() {
    return totalCountsRepo
      .findTopByOrderByTimestampDesc()
      .map(tc -> TotalCounts
        .builder()
        .files(tc.getFiles())
        .samples(tc.getSamples())
        .studies(tc.getStudies())
        .fileSizeBytes(tc.getFileSizeBytes())
        .fileSizeHumanReadable(tc.getFileSizeHumanReadable())
        .timestamp(tc.getTimestamp())
        .build());
  }

  private Disposable createTotalCountsPipelineDisposable() {
    return eventEmitter
        .receive()
        .doOnNext(
            instant -> {
              log.info("totalCountsDisposable received instant: {}", instant);

              if (!calculatorDisposable.isDisposed()) {
                log.info("Killing previous calculator disposable!");
                this.calculatorDisposable.dispose();
              }

              log.info("Spawning new calculator disposable...");
              this.calculatorDisposable = createCalculatorDisposable(instant);
            })
        .subscribe();
  }

  private Disposable createCalculatorDisposable(Instant instant) {
    log.info("starting totalCounts:");
    return createBuilderWithStudiesAndFilesCount()
        .flatMap(
            totalCountsBuilder ->
                countUniqueGenomes().map(count -> totalCountsBuilder.samples(count.longValue())))
        .map(totalCountsBuilder -> totalCountsBuilder.timestamp(instant.toEpochMilli()))
        .map(TotalCounts.TotalCountsBuilder::build)
        .flatMap(this::updateTotalCount)
        .subscribe(tc -> log.info("CachedTotalCounts calculated - " + tc.toString()));
  }

  private Mono<TotalCounts.TotalCountsBuilder> createBuilderWithStudiesAndFilesCount() {
    val STUDIES_AGG_NAME = "studies";
    val FILES_AGG_NAME = "files";
    val source =
        new SearchSourceBuilder()
            .aggregation(
                AggregationBuilders.terms(STUDIES_AGG_NAME)
                    .field(FIELD_STUDY_ID)
                    .size(MAX_AGGREGATE_BUCKETS))
            .aggregation(AggregationBuilders.stats(FILES_AGG_NAME).field(FIELD_FILE_SIZE))
            .fetchSource(false);

    return client
        .aggregate(new SearchRequest().indices(properties.getFileCentricIndex()).source(source))
        .collectMap(Aggregation::getName)
        .map(
            aggLookUp -> {
              val builder = TotalCounts.builder();
              if (aggLookUp.containsKey(STUDIES_AGG_NAME)) {
                val agg = (ParsedStringTerms) aggLookUp.get(STUDIES_AGG_NAME);
                builder.studies(agg.getBuckets().size());
              }
              if (aggLookUp.containsKey(FILES_AGG_NAME)) {
                val agg = (ParsedStats) aggLookUp.get(FILES_AGG_NAME);
                builder.files(agg.getCount());
                Double sum = agg.getSum();
                // sum is a whole number because we index in bytes so longValue will have no data
                // loss
                builder.fileSizeBytes(sum.longValue());
                builder.fileSizeHumanReadable(convertBytesToHumanReadable(sum.longValue()));
              }
              return builder;
            });
  }

  private Mono<Integer> countUniqueGenomes() {
    return client
        .scroll(
            new SearchRequest()
                .indices(properties.getFileCentricIndex())
                .source(
                    new SearchSourceBuilder()
                        .query(QueryBuilders.rangeQuery(LAST_UPDATED_AT_FIELD).to(Instant.now()))
                        .size(SIZE)
                        .fetchSource(ES_INCLUDE, null))
                .scroll(new TimeValue(properties.getScrollTimeoutMinutes(), TimeUnit.MINUTES)))
        .flatMap(this::extractSubmitterDonorIdsFromSearchHit)
        // genomes are currently in 100,000s range and Integer max is ~2billion;
        .reduce(
            new HashSet<String>(),
            (hashSet, id) -> {
              hashSet.add(id);
              return hashSet;
            })
        .map(HashSet::size);
  }

  private Flux<String> extractSubmitterDonorIdsFromSearchHit(SearchHit searchHit) {
    Map<String, Object> source = searchHit.getSourceAsMap();
    if (source.get(FIELD_DONORS) instanceof List) {
      List<Object> donors = (List<Object>) source.getOrDefault(FIELD_DONORS, new ArrayList<>());
      return Flux.fromStream(
          donors.stream()
              .map(
                  d -> {
                    if (d instanceof Map) {
                      val castedD = (Map<String, Object>) d;
                      return castedD.getOrDefault(FIELD_SUBMITTER_DONOR_ID, "").toString();
                    }
                    return "";
                  })
              .filter(id -> !id.isEmpty()));
    }
    return Flux.empty();
  }

  private Mono<TotalCounts> updateTotalCount(TotalCounts totalCounts){
      return totalCountsRepo
        .findTopByOrderByTimestampDesc()
        .flatMap(o -> {
          if (!o.getFiles().equals(totalCounts.getFiles())
            || !o.getSamples().equals(totalCounts.getSamples())
            || !o.getStudies().equals(totalCounts.getStudies())
            || !o.getFileSizeBytes().equals(totalCounts.getFileSizeBytes())){
            log.info("Updating database with totalCounts");
            return totalCountsRepo.save(totalCounts);
          } else {
            return Mono.just(o);
          }
        }).switchIfEmpty(totalCountsRepo.save(totalCounts));
  }
}
