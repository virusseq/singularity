package org.cancogenvirusseq.singularity.repository.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Table("archive_set_query")
public class ArchiveSetQuery extends ArchiveAll {
  @NonNull private String setQueryHash;

  @RequiredArgsConstructor
  public enum Fields {
    meta("meta"),
    setQueryHash("set_query_hash"),
    timestamp("timestamp"),
    status("status"),
    type("type"),
    objectId("object_id"),
    id("id");

    private final String text;

    public String toString() {
      return text;
    }
  }
}
