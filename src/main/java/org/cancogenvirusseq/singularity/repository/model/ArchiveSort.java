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

  public static final ArchiveSort<ArchiveAll.Fields> DEFAULT_ARCHIVE_ALL_SORT =
      ArchiveSort.<ArchiveAll.Fields>builder()
          .fieldName(ArchiveAll.Fields.timestamp)
          .sortDirection(Sort.Direction.ASC)
          .build();

  public static final ArchiveSort<ArchiveSetQuery.Fields> DEFAULT_ARCHIVE_SET_QUERY_SORT =
      ArchiveSort.<ArchiveSetQuery.Fields>builder()
          .fieldName(ArchiveSetQuery.Fields.timestamp)
          .sortDirection(Sort.Direction.ASC)
          .build();
}
