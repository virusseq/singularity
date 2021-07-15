package org.cancogenvirusseq.singularity.repository.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Table("archive")
public class Archive {
  @Id private UUID id;
  @NonNull private ArchiveStatus status;
  @NonNull private ArchiveType type;
  @NonNull private String hashInfo;
  private String hash;
  private UUID objectId;

  private Long created_at;

  @NonNull private Integer numOfSamples;
  @NonNull private Integer numOfDownloads;

  @RequiredArgsConstructor
  public enum Fields {
    createdAt("created_at"),
    status("status"),
    type("type"),
    objectId("object_id"),
    id("id"),
    numOfSamples("num_of_samples"),
    numOfDownloads("num_of_downloads"),
    hash("hash");

    private final String text;

    public String toString() {
      return text;
    }
  }
}
