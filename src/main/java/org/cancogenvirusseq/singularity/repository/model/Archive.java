package org.cancogenvirusseq.singularity.repository.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
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
  @Id private Long timestamp;
  @NonNull private ArchiveStatus status;
  @NonNull private Long numOfSamples;
  @NonNull private String name;

  public enum Fields {
    timestamp,
    status,
    numOfSamples,
    name
  }
}
