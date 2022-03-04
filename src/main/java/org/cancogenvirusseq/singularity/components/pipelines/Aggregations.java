package org.cancogenvirusseq.singularity.components.pipelines;

import lombok.val;
import org.cancogenvirusseq.singularity.components.events.EventEmitter;
import org.cancogenvirusseq.singularity.components.model.TotalCounts;
import org.cancogenvirusseq.singularity.config.elasticsearch.ElasticsearchProperties;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.metrics.ParsedStats;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
public class Aggregations {
    private final String fileIndex;
    private final ReactiveElasticsearchClient client;
    private final EventEmitter<Instant> eventEmitter;

    private static final Integer MAX_AGGREGATE_BUCKETS = 1000;

    public Aggregations(EventEmitter<Instant> eventEmitter,
                        ReactiveElasticsearchClient client,
                        ElasticsearchProperties properties) {
        this.eventEmitter = eventEmitter;
        this.client = client;
        this.fileIndex = properties.getFileCentricIndex();
//        cached = TotalCounts.builder().build();
    }

    public Mono<TotalCounts> getAggregationCounts() {
        val source =
                new SearchSourceBuilder()
                        .aggregation(
                                AggregationBuilders.terms("studies")
                                        .field("study_id")
                                        .size(MAX_AGGREGATE_BUCKETS))
                        .aggregation(
                                AggregationBuilders.stats("files")
                                        .field("file.size"))
                        .fetchSource(false);

        return client.aggregate(new SearchRequest()
                                .indices(fileIndex)
                                .source(source))
               .collectMap(Aggregation::getName)
               .map(aggLookUp -> {
                   val builder = TotalCounts.builder();
                    if (aggLookUp.containsKey("studies")) {
                        val agg = (ParsedStringTerms) aggLookUp.get("studies");
                        builder.studies(agg.getBuckets().size());
                    }
                    if (aggLookUp.containsKey("files")) {
                        val agg = (ParsedStats) aggLookUp.get("files");
                        builder.files(agg.getCount());
                        builder.fileSize(agg.getSum());
                    }
                    return builder.build();
                });
    }
}
