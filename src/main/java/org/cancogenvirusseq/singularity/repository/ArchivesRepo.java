package org.cancogenvirusseq.singularity.repository;

import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.repository.model.ArchiveStatus;
import org.cancogenvirusseq.singularity.repository.model.ArchiveType;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ArchivesRepo extends ArchiveCustomRepo, ReactiveCrudRepository<Archive, UUID> {
  Mono<Long> countByStatusAndType(ArchiveStatus status, ArchiveType type);
}
