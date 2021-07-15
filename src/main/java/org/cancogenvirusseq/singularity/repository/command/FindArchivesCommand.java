package org.cancogenvirusseq.singularity.repository.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.repository.model.ArchiveStatus;
import org.cancogenvirusseq.singularity.repository.model.ArchiveType;
import org.springframework.data.domain.Sort;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindArchivesCommand {
  @NonNull Integer page = 0;
  @NonNull Integer size = 20;
  @NonNull Sort.Direction sortDirection = Sort.Direction.ASC;
  @NonNull Archive.Fields sortField = Archive.Fields.createdAt;
  @NonNull ArchiveStatus status = ArchiveStatus.COMPLETE;
  @NonNull ArchiveType type = ArchiveType.ALL;
  @NonNull Long fromCreateTimeEpoch = 0L;
  @NonNull Long toCreateTimeEpoch = Instant.now().toEpochMilli();
}
