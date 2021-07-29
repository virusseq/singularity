package org.cancogenvirusseq.singularity.components.model;

import java.time.Instant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.elasticsearch.index.query.QueryBuilder;

@Getter
@RequiredArgsConstructor
public class ArchiveBuildRequest {
  private final Archive archive;
  private final QueryBuilder queryBuilder;
  private final Instant instant;
}
