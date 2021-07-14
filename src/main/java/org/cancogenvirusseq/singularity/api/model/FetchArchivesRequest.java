package org.cancogenvirusseq.singularity.api.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.cancogenvirusseq.singularity.repository.model.ArchiveAll;
import org.cancogenvirusseq.singularity.repository.model.ArchiveStatus;
import org.springframework.data.domain.Sort;

@Data
@NoArgsConstructor
public class FetchArchivesRequest {
  ArchiveStatus status;
  Integer offset = 0;
  Integer size = 20;
  Sort.Direction sortDirection = Sort.Direction.ASC;
  ArchiveAll.Fields sortField = ArchiveAll.Fields.timestamp;
}
