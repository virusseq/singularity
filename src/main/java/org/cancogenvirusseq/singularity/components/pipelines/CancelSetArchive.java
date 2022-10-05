package org.cancogenvirusseq.singularity.components.pipelines;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.api.model.CancelListResponse;
import org.cancogenvirusseq.singularity.api.model.ErrorArchive;
import org.cancogenvirusseq.singularity.api.model.HashResult;
import org.cancogenvirusseq.singularity.api.model.Summary;
import org.cancogenvirusseq.singularity.config.archive.ArchiveProperties;
import org.cancogenvirusseq.singularity.repository.ArchivesRepo;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.repository.model.ArchiveStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.*;
import java.util.function.BiFunction;


@Slf4j
@Component
@RequiredArgsConstructor
public class CancelSetArchive implements BiFunction<Collection<String>, Boolean, Mono<CancelListResponse>> {

  private final ArchivesRepo archivesRepo;

  private final ArchiveProperties archiveProperties;

  @Override
  public Mono<CancelListResponse> apply(Collection<String> hashList, Boolean force) {

    List<ErrorArchive> errorList = new ArrayList<>();
    List<String> ignoredList = new ArrayList<>(hashList);
    Map<String, HashResult> hashResultMap = new HashMap<>();

    return Mono.just(hashList)
      .flatMapMany(hl -> this.getArchivesToCancel(hl, force))
      .doOnNext(a ->
        hashResultMap.put(
          a.getHash(),
          HashResult
            .builder()
            .hash(a.getHash())
            .oldStatus(a.getStatus().toString())
            .createdAt(new Date(a.getCreatedAt() * 1000).toString())
            .type(a.getType().name())
            .numberOfSamples(a.getNumOfSamples())
            .build()
        ))
      .doOnNext(a -> ignoredList.remove(a.getHash()))
      .flatMap(a -> {
        a.setStatus(ArchiveStatus.CANCELLED);
        return archivesRepo
          .save(a)
          .flatMap(archivesRepo::findByArchiveObject)
          .doOnSuccess(savedArchive -> hashResultMap.get(savedArchive.getHash()).setNewStatus(savedArchive.getStatus().toString()))
          .onErrorResume(err -> {
            errorList.add(new ErrorArchive(a.getHash(), null, err.getMessage()));
            hashResultMap.remove(a.getHash());
            return Mono.empty();
          });
      })
      .collectList()
      .map(al -> new CancelListResponse(
        new ArrayList<>(hashResultMap.values()),
        (errorList.size() > 0) ? errorList : null,
        (ignoredList.size() > 0) ? ignoredList : null,
        new Summary(
          hashResultMap.size(),
          errorList.size(),
          ignoredList.size(),
          (hashList.size() > 0) ? hashList.size() : al.size()
        )
      ))
      .log();
  }

  private Flux<Archive> getArchivesToCancel(Collection<String> strings, Boolean force) {
    return Mono.just(strings)
      .flatMapMany(
        s -> (s.size() > 0)
          ? searchBuildingArchivesByIds(s, force)
          : searchBuildingArchives(force)
    ).log();
  }

  private Flux<Archive> searchBuildingArchivesByIds(Collection<String> strings, Boolean force) {
    return (force) ?
      archivesRepo.findBuildingArchivesByHashList(new ArrayList<>(strings)) :
      archivesRepo.findBuildingArchivesByHashListOlderThan(new ArrayList<>(strings), Instant.now().minusSeconds(archiveProperties.getCancelPeriodSeconds()).getEpochSecond());
  }

  private Flux<Archive> searchBuildingArchives(Boolean force) {
    return (force) ?
      archivesRepo.findBuildingArchives() :
      archivesRepo.findBuildingArchivesOlderThan(Instant.now().minusSeconds(archiveProperties.getCancelPeriodSeconds()).getEpochSecond());
  }

}
