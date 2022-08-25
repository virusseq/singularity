package org.cancogenvirusseq.singularity.components.pipelines;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.api.model.CancelListResponse;
import org.cancogenvirusseq.singularity.api.model.ErrorArchive;
import org.cancogenvirusseq.singularity.api.model.HashResult;
import org.cancogenvirusseq.singularity.api.model.Summary;
import org.cancogenvirusseq.singularity.repository.ArchivesRepo;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.repository.model.ArchiveStatus;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;


@Slf4j
@Component
@RequiredArgsConstructor
@ConfigurationProperties("archive")
public class CancelSetArchive implements Function<Collection<String>, Mono<CancelListResponse>> {

  private final ArchivesRepo archivesRepo;

  // config value
  @Setter
  private long cancelPeriodSeconds;

  @Override
  public Mono<CancelListResponse> apply(Collection<String> strings) {

    List<ErrorArchive> errorList = new ArrayList<>();
    Map<String, HashResult> hashResultMap = new HashMap<>();

    return Mono.just(strings)
      .flatMapMany(this::getArchivesToCancel)
      .doOnNext(a ->
        hashResultMap.put(
          a.getHash(),
          new HashResult(a.getHash(), a.getStatus().toString(), null, new Date(a.getCreatedAt() * 1000).toString())))
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
        new ArrayList<HashResult>(hashResultMap.values()),
        errorList,
        new Summary(
          hashResultMap.size(),
          errorList.size(),
          al.size() - hashResultMap.size() - errorList.size()
        )
      ))
      .log();
  }

  private Flux<Archive> getArchivesToCancel(Collection<String> strings) {
    return Mono.just(strings)
      .flatMapMany(
        s -> (s.size() > 0)
          ? searchBuildingArchivesByIds(s)
          : searchBuildingArchives()
    ).log();
  }

  private Flux<Archive> searchBuildingArchivesByIds(Collection<String> strings) {
    return archivesRepo
      .findBuildingArchivesByHashList(new ArrayList<>(strings));
  }

  private Flux<Archive> searchBuildingArchives() {
    return archivesRepo
      .findBuildingArchivesOlderThan(Instant.now().minusSeconds(cancelPeriodSeconds).toEpochMilli());
  }

}
