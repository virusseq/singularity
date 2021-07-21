package org.cancogenvirusseq.singularity.repository.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Table("archive")
public class Archive {
  @Id private UUID id;
  @NonNull private ArchiveStatus status;
  @NonNull private ArchiveType type;
  @NonNull private String hashInfo;
  private String hash;
  private UUID objectId;
  private Long createdAt;

  @NonNull private Integer numOfSamples;
  @NonNull private Integer numOfDownloads;

  @RequiredArgsConstructor
  public enum Fields {
    createdAt("createdAt"),
    status("status"),
    type("type"),
    objectId("objectId"),
    id("id"),
    numOfSamples("numOfSamples"),
    numOfDownloads("numOfDownloads"),
    hash("hash");

    private final String text;

    public String toString() {
      return text;
    }
  }
}
