package org.cancogenvirusseq.singularity.api.model;

import lombok.*;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.repository.model.ArchiveStatus;
import org.cancogenvirusseq.singularity.repository.model.ArchiveType;
import org.springframework.data.domain.Sort;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FetchArchivesRequest {
  @NonNull  Integer page = 0;
  @NonNull  Integer size = 20;
  @NonNull  Sort.Direction sortDirection = Sort.Direction.ASC;
  @NonNull  Archive.Fields sortField = Archive.Fields.createdAt;
  @NonNull ArchiveStatus status = ArchiveStatus.COMPLETE;
  @NonNull ArchiveType type = ArchiveType.ALL;
}
