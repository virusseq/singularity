package org.cancogenvirusseq.singularity.repository;

import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.repository.model.ArchiveStatus;
import org.cancogenvirusseq.singularity.repository.model.ArchiveType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ArchivesRepo extends ReactiveCrudRepository<Archive, UUID> {
    Flux<Archive> findByStatusAndType(ArchiveStatus status, ArchiveType type, Pageable pageable);

    Mono<Integer> countByStatusAndType(ArchiveStatus status, ArchiveType type);
}
