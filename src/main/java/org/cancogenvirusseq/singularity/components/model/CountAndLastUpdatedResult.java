package org.cancogenvirusseq.singularity.components.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.search.aggregations.metrics.ParsedMax;
import org.elasticsearch.search.aggregations.metrics.ParsedValueCount;

@Getter
@RequiredArgsConstructor
public class CountAndLastUpdatedResult {
  private final ParsedMax lastUpdatedDate;
  private final ParsedValueCount numDocuments;
}
