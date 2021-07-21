package org.cancogenvirusseq.singularity.repository.query;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.repository.model.ArchiveStatus;
import org.cancogenvirusseq.singularity.repository.model.ArchiveType;
import org.springframework.data.domain.Sort;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindArchivesQuery {
  @NonNull Integer page = 0;
  @NonNull Integer size = 20;
  @NonNull Sort.Direction sortDirection = Sort.Direction.ASC;
  @NonNull Archive.Fields sortField = Archive.Fields.createdAt;
  @NonNull ArchiveStatus status = ArchiveStatus.COMPLETE;
  @NonNull ArchiveType type = ArchiveType.ALL;
  @NonNull Long createdAfterEpochSec = 0L;
  @NonNull Long createdBeforeEpochSec = Instant.now().toEpochMilli();
}
