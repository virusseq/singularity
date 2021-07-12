package org.cancogenvirusseq.singularity.repository.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Table("archive")
public class Archive {
  @Id private UUID id;
  @NonNull private ArchiveStatus status;
  @NonNull private ArchiveType type;
  @NonNull private Long timestamp;
  @NonNull private Integer numOfSamples;
  private String setId;
  private UUID objectId;
  private ArchiveMeta meta;

  @RequiredArgsConstructor
  public enum Fields {
    timestamp("timestamp"),
    status("status"),
    numOfSamples("num_of_samples"),
    type("type"),
    objectId("object_id"),
    setId("set_id"),
    id("id");

    private final String text;

    public String toString() {
      return text;
    }
  }
}
