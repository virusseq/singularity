package org.cancogenvirusseq.singularity.repository;

import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.repository.model.ArchiveStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ArchivesRepo extends ReactiveSortingRepository<Archive, Long> {
  Flux<Archive> findAllByStatus(ArchiveStatus status, Pageable pageable);

  Mono<Long> countByStatus(ArchiveStatus status);
}
