package org.cancogenvirusseq.singularity.components.pipelines;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.api.model.EntityListResponse;
import org.cancogenvirusseq.singularity.api.model.HashResult;
import org.cancogenvirusseq.singularity.repository.ArchivesRepo;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class CancelSetArchive implements Function<Collection<String>, Mono<EntityListResponse>> {

  private final ArchivesRepo archivesRepo;

  @Override
  public Mono<EntityListResponse> apply(Collection<String> strings) {
    // ArchiveRepo.findArchivesByHashIn(strings)
    // ArchiveRepo.saveAll(*)
    // return OK if success
    return Mono.just(strings)
      .flatMap(this::searchByIds)
      .flatMap(this::toHashResultList)
      .flatMap(this::toEntityListResponse);
  }

  private Mono<Collection<String>> searchByIds(Collection<String> strings) {
    // TODO: search in the Database
    // Flux<Archive> archivesByHashIn = archivesRepo.findArchivesByHashIn(new ArrayList<>(strings));
    return Mono.just(strings);
  }

  private Mono<EntityListResponse>toEntityListResponse(List<HashResult> hashResult) {
    // parsing List object into EntityListResponse
    return Mono
      .just(EntityListResponse.builder().data(List.of(hashResult)).build());
  }

  private Mono<List<HashResult>> toHashResultList(Collection<String> s) {
    // parsing Collection of Strings into List
    return Flux
      .fromIterable(s)
      .map(str -> HashResult.builder().hash(str).build())
      .collectList();
  }
}
