package org.cancogenvirusseq.singularity.repository;

import java.util.UUID;
import lombok.val;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.repository.model.ArchiveStatus;
import org.cancogenvirusseq.singularity.repository.model.ArchiveType;
import org.cancogenvirusseq.singularity.repository.query.FindArchivesQuery;
import org.springframework.data.domain.*;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ArchivesRepo extends ReactiveCrudRepository<Archive, UUID> {

  Flux<Archive> findByStatusAndTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqual(
      ArchiveStatus status, ArchiveType type, Long fromTime, Long toTime, Pageable pageable);

  Mono<Integer> countByStatusAndTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqual(
      ArchiveStatus status, ArchiveType type, Long fromTime, Long toTime);

  default Mono<Archive> findByArchiveObject(Archive archive) {
    return findById(archive.getId());
  }

  default Mono<Page<Archive>> findByCommand(FindArchivesQuery findArchivesQuery) {
    val status = findArchivesQuery.getStatus();
    val type = findArchivesQuery.getType();
    val fromTime = findArchivesQuery.getCreatedAfterEpochSec();
    val toTime = findArchivesQuery.getCreatedBeforeEpochSec();
    val pageable =
        PageRequest.of(
            findArchivesQuery.getPage(),
            findArchivesQuery.getSize(),
            Sort.by(
                findArchivesQuery.getSortDirection(), findArchivesQuery.getSortField().toString()));

    val totalHitsMono =
        countByStatusAndTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqual(
            status, type, fromTime, toTime);

    return findByStatusAndTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqual(
            status, type, fromTime, toTime, pageable)
        .collectList()
        .zipWith(
            totalHitsMono, (archives, totalHits) -> new PageImpl<>(archives, pageable, totalHits));
  }
}
