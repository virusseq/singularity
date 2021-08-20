package org.cancogenvirusseq.singularity.components.hoc;

import static org.cancogenvirusseq.singularity.components.model.AnalysisDocument.LAST_UPDATED_AT_FIELD;

import java.time.Instant;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.components.base.CountAndLastUpdatedAggregation;
import org.cancogenvirusseq.singularity.components.model.ArchiveBuildRequest;
import org.cancogenvirusseq.singularity.components.model.CountAndLastUpdatedResult;
import org.cancogenvirusseq.singularity.repository.ArchivesRepo;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.repository.model.ArchiveStatus;
import org.cancogenvirusseq.singularity.repository.model.ArchiveType;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstantToArchiveBuildRequest implements Function<Instant, Mono<ArchiveBuildRequest>> {
  private final CountAndLastUpdatedAggregation countAndLastUpdatedAggregation;
  private final ArchivesRepo archivesRepo;

  @Override
  public Mono<ArchiveBuildRequest> apply(Instant instant) {
    return Mono.just(QueryBuilders.rangeQuery(LAST_UPDATED_AT_FIELD).to(instant))
        .flatMap(countAndLastUpdatedAggregation)
        .flatMap(this::createAndSaveArchiveToDatabase)
        // why this? because R2DBC does not hydrate fields
        // (https://github.com/spring-projects/spring-data-r2dbc/issues/455)
        .flatMap(archivesRepo::findByArchiveObject)
        .map(transformToArchiveBuildRequest(instant))
        .onErrorStop()
        .log("InstantToArchiveBuildRequest");
  }

  private Mono<Archive> createAndSaveArchiveToDatabase(
      CountAndLastUpdatedResult countAndLastUpdatedResult) {
    return archivesRepo.save(
        Archive.builder()
            .status(ArchiveStatus.BUILDING)
            .type(ArchiveType.ALL)
            .hashInfo(countAndLastUpdatedResult.getLastUpdatedDate().getValueAsString())
            .numOfSamples(countAndLastUpdatedResult.getNumDocuments().getValue())
            .build());
  }

  private Function<Archive, ArchiveBuildRequest> transformToArchiveBuildRequest(Instant instant) {
    return archive ->
        new ArchiveBuildRequest(
            archive, QueryBuilders.rangeQuery(LAST_UPDATED_AT_FIELD).to(instant));
  }
}
