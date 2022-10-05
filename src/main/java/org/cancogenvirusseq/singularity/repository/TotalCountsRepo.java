package org.cancogenvirusseq.singularity.repository;

import org.cancogenvirusseq.singularity.components.model.TotalCounts;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface TotalCountsRepo extends ReactiveCrudRepository<TotalCounts, UUID> {
  Mono<TotalCounts> findTopByOrderByTimestampDesc();
}
