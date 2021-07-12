package org.cancogenvirusseq.singularity.api.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.repository.model.ArchiveStatus;
import org.cancogenvirusseq.singularity.repository.model.ArchiveType;
import org.springframework.data.domain.Sort;

@Data
@NoArgsConstructor
public class FetchArchivesRequest {
  ArchiveStatus status = ArchiveStatus.COMPLETE;
  ArchiveType type = ArchiveType.ALL;
  Integer page = 0;
  Integer size = 20;
  Sort.Direction sortDirection = Sort.Direction.ASC;
  Archive.Fields sortField = Archive.Fields.timestamp;
}
