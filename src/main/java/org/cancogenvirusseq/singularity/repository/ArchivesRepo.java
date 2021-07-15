package org.cancogenvirusseq.singularity.repository;

import java.util.UUID;

import lombok.val;
import org.cancogenvirusseq.singularity.repository.command.FindArchivesCommand;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.repository.model.ArchiveStatus;
import org.cancogenvirusseq.singularity.repository.model.ArchiveType;
import org.springframework.data.domain.*;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ArchivesRepo extends ReactiveCrudRepository<Archive, UUID> {

  Flux<Archive> findByStatusAndTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqual(ArchiveStatus status, ArchiveType type, Long fromTime, Long toTime, Pageable pageable);

  Mono<Integer> countByStatusAndTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqual(ArchiveStatus status, ArchiveType type, Long fromTime, Long toTime);

  default Mono<Page<Archive>> findByCommand(FindArchivesCommand findArchivesCommand) {
    val status = findArchivesCommand.getStatus();
    val type = findArchivesCommand.getType();
    val fromTime = findArchivesCommand.getFromCreateTimeEpoch();
    val toTime = findArchivesCommand.getToCreateTimeEpoch();
    val pageable =
            PageRequest.of(
                    findArchivesCommand.getPage(),
                    findArchivesCommand.getSize(),
                    Sort.by(
                            findArchivesCommand.getSortDirection(),
                            findArchivesCommand.getSortField().toString()));

    val totalHitsMono = countByStatusAndTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqual(status, type, fromTime, toTime);

    return findByStatusAndTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqual(status, type, fromTime, toTime, pageable)
                   .collectList()
                   .zipWith(
                           totalHitsMono, (archives, totalHits) -> new PageImpl<>(archives, pageable, totalHits));
  }
}
