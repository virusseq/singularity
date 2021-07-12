package org.cancogenvirusseq.singularity.repository.model;

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
@Table("archive")
public class Archive {
  @Id private UUID id;
  @NonNull private ArchiveStatus status;
  @NonNull private ArchiveType type;
  @NonNull private Long timestamp;
  @NonNull private Long numOfSamples;
  private String setId;
  private UUID objectId;

  public enum Fields {
    timestamp,
    status,
    numOfSamples,
    name,
    type,
    objectId,
    setId,
    id
  }
}
