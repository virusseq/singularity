package org.cancogenvirusseq.singularity.components.utils;

import static org.cancogenvirusseq.singularity.components.utils.PostgresUtils.getSqlStateOptionalFromException;
import static org.cancogenvirusseq.singularity.components.utils.PostgresUtils.isUniqueViolationError;

import java.time.Instant;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.components.notifications.archives.ArchiveNotifier;
import org.cancogenvirusseq.singularity.config.archive.ArchiveProperties;
import org.cancogenvirusseq.singularity.exceptions.runtime.ExistingArchiveRestartException;
import org.cancogenvirusseq.singularity.repository.ArchivesRepo;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.repository.model.ArchiveStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExistingArchiveUtils {
  private final ArchiveProperties archiveProperties;
  private final ArchivesRepo archivesRepo;
  private final ArchiveNotifier notifier;

  /**
   * Saves new archive in database, with logic for handling hash collisions:
   * If the archive is found in DB (hash collision) then we will check if it can be restarted,
   * if it can, we will reset its status to building and give it the current time as createdAt time,
   * then return the existing archive.
   * @param archive
   * @return
   */
  public Mono<Archive> createNewOrResetExistingArchiveInDatabase(Archive archive) {
    return archivesRepo
        .save(archive)
        // why this? because R2DBC does not hydrate fields
        // (https://github.com/spring-projects/spring-data-r2dbc/issues/455)
        .flatMap(archivesRepo::findByArchiveObject)
        .doOnSuccess((newArchive) -> {
          notifier.notify(newArchive);
        })
        .onErrorResume(
            DataIntegrityViolationException.class,
            // Save failed due to our uniqueness constraints
            dataViolation ->
                getSqlStateOptionalFromException(dataViolation)
                    .filter(isUniqueViolationError)
                    .map(
                        // we got a hash collision (uniqueViolationError)!!
                        // this is the expected case, lets run our code to find archives by the
                        // hashInfo
                        // and reset their status so we can use the existing record (if it can be
                        // restarted).
                        uniqueConstraint ->
                            archivesRepo
                                .findArchiveByHashInfoEquals(archive.getHashInfo())
                                .flatMap(this.canBeRestarted())
                                .flatMap(this.resetArchiveForRestart()))
                    .orElseThrow(() -> dataViolation)
                    .log());
  }

  public Function<Archive, Mono<Archive>> canBeRestarted() {
    return archive ->
        (ArchiveStatus.CANCELLED.equals(archive.getStatus())
                || ArchiveStatus.FAILED.equals(archive.getStatus())
                || (ArchiveStatus.BUILDING.equals(archive.getStatus())
                    && archive.getCreatedAt()
                        < Instant.now()
                            .minusSeconds(archiveProperties.getMaxBuildingSeconds())
                            .getEpochSecond()))
            ? Mono.just(archive)
            : Mono.error(new ExistingArchiveRestartException(archive));
  }

  /**
   *
   */
  public Function<Archive, Mono<Archive>> resetArchiveForRestart() {
    return existingArchive -> {
      // reset the status of existing archive, save and return as mono
      log.info("Restart building archive hash:{}, previous status was {} created at {}",
              existingArchive.getHash(), existingArchive.getStatus(), existingArchive.getCreatedAt());
      existingArchive.setStatus(ArchiveStatus.BUILDING);
      existingArchive.setCreatedAt(Instant.now().getEpochSecond());
      notifier.notify(existingArchive);
      return archivesRepo.save(existingArchive);
    };
  }
}
