package org.cancogenvirusseq.singularity.components.hoc;

import static org.cancogenvirusseq.singularity.components.model.AnalysisDocument.LAST_UPDATED_AT_FIELD;

import java.time.Instant;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.components.base.CountAndLastUpdatedAggregation;
import org.cancogenvirusseq.singularity.components.model.ArchiveBuildRequest;
import org.cancogenvirusseq.singularity.components.model.CountAndLastUpdatedResult;
import org.cancogenvirusseq.singularity.components.utils.ExistingArchiveUtils;
import org.cancogenvirusseq.singularity.repository.ArchivesRepo;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstantToArchiveBuildRequest implements Function<Instant, Mono<ArchiveBuildRequest>> {
    private final CountAndLastUpdatedAggregation countAndLastUpdatedAggregation;
    private final ArchivesRepo archivesRepo;

    private final ExistingArchiveUtils existingArchiveUtils;

    @Override
    public Mono<ArchiveBuildRequest> apply(Instant instant) {
        return Mono.just(QueryBuilders.rangeQuery(LAST_UPDATED_AT_FIELD).to(instant))
            .flatMap(countAndLastUpdatedAggregation)
            .flatMap(this::createOrGetArchiveInDatabase)
            // why this? because R2DBC does not hydrate fields
            // (https://github.com/spring-projects/spring-data-r2dbc/issues/455)
            .flatMap(archivesRepo::findByArchiveObject)
            .map(transformToArchiveBuildRequest(instant))
            .onErrorStop()
            .log("InstantToArchiveBuildRequest");
    }

  /**
   * Save archive to DB to track progress, if this fails due to hash collision, check if the
   * existing archive instead should be used instead, restarting that build
   *
   * @param countAndLastUpdatedResult
   * @return
   */
    private Mono<Archive> createOrGetArchiveInDatabase(CountAndLastUpdatedResult countAndLastUpdatedResult) {
        Archive archiveTemplate = Archive.newAllArchiveFromCountAndLastUpdatedResult(countAndLastUpdatedResult);

        return existingArchiveUtils.createNewOrResetExistingArchiveInDatabase(archiveTemplate);
    }

    private Function<Archive, ArchiveBuildRequest> transformToArchiveBuildRequest(Instant instant) {
        return archive ->
            // include files up to this instant (don't include things added after this starts)
            new ArchiveBuildRequest(archive, QueryBuilders.rangeQuery(LAST_UPDATED_AT_FIELD).to(instant));
    }
}
