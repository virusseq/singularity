package org.cancogenvirusseq.singularity.components;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.cancogenvirusseq.singularity.api.model.FetchArchivesRequest;
import org.cancogenvirusseq.singularity.repository.ArchivesRepo;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class Archives {
  private final ArchivesRepo archivesRepo;

  public Mono<Archive> getArchiveById(UUID id) {
    return archivesRepo.findById(id);
  }

  public Mono<Page<Archive>> getArchivesWithStatus(FetchArchivesRequest fetchArchivesRequest) {
    val status = fetchArchivesRequest.getStatus();
    val type = fetchArchivesRequest.getType();
    val pageable =
        PageRequest.of(
            fetchArchivesRequest.getPage(),
            fetchArchivesRequest.getSize(),
            Sort.by(
                fetchArchivesRequest.getSortDirection(),
                fetchArchivesRequest.getSortField().toString()));
    val totalHitsMono = archivesRepo.countByStatusAndType(status, type);
    return archivesRepo
        .findByStatusAndType(status, type, pageable)
        .collectList()
        .zipWith(
            totalHitsMono, (archives, totalHits) -> new PageImpl<>(archives, pageable, totalHits));
  }
}
