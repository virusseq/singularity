package org.cancogenvirusseq.singularity.repository;

import java.util.UUID;
import org.cancogenvirusseq.singularity.repository.model.ArchiveAll;
import org.cancogenvirusseq.singularity.repository.model.ArchiveStatus;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ArchiveAllRepo extends ReactiveCrudRepository<ArchiveAll, UUID> {
  Mono<Long> countByStatus(ArchiveStatus status);
}
