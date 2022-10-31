package org.cancogenvirusseq.singularity.components.utils;

import static org.cancogenvirusseq.singularity.components.utils.PostgresUtils.getSqlStateOptionalFromException;
import static org.cancogenvirusseq.singularity.components.utils.PostgresUtils.isUniqueViolationError;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.components.notifications.IndexerNotification;
import org.cancogenvirusseq.singularity.components.notifications.Message;
import org.cancogenvirusseq.singularity.components.notifications.NotificationName;
import org.cancogenvirusseq.singularity.components.notifications.Notifier;
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
  private final Notifier notifier;

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
        .doOnSuccess((newArchive) -> {
          Message message = new Message(newArchive.getStatus(), newArchive.getHash(), new Date(TimeUnit.SECONDS.toMillis(newArchive.getCreatedAt())));
          notifier.notify(new IndexerNotification(NotificationName.BUILDING_RELEASE, message.toLinkedHashMap()));
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
      Message message = new Message(existingArchive.getStatus(), existingArchive.getHash(), new Date(TimeUnit.SECONDS.toMillis(existingArchive.getCreatedAt())));
      notifier.notify(new IndexerNotification(NotificationName.BUILDING_RELEASE, message.toLinkedHashMap()));
      return archivesRepo.save(existingArchive);
    };
  }
}
