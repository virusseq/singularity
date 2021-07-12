package org.cancogenvirusseq.singularity.components;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.cancogenvirusseq.singularity.api.model.FetchArchivesRequest;
import org.cancogenvirusseq.singularity.repository.ArchivesRepo;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class Archives {
  public final ArchivesRepo archivesRepo;

  public Mono<Page<Archive>> getArchivesWithStatus(FetchArchivesRequest fetchArchivesRequest) {
    val type = fetchArchivesRequest.getType();
    val status = fetchArchivesRequest.getStatus();

    val pageable =
        PageRequest.of(
            fetchArchivesRequest.getPage(),
            fetchArchivesRequest.getSize(),
            Sort.by(
                fetchArchivesRequest.getSortDirection(),
                fetchArchivesRequest.getSortField().toString()));

    val totalHitsMono = archivesRepo.countByStatusAndType(status, type);
    return archivesRepo
        .findAll()
        .collectList()
        .zipWith(
            totalHitsMono, (archives, totalHits) -> new PageImpl<>(archives, pageable, totalHits));
  }
}
