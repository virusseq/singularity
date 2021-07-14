package org.cancogenvirusseq.singularity.repository.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.springframework.data.domain.Sort;

@Value
@Builder
public class ArchiveSort<Field> {
  @NonNull Field fieldName;
  @NonNull Sort.Direction sortDirection;
}
