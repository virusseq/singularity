package org.cancogenvirusseq.singularity.repository.model;

import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@AllArgsConstructor
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
  private Integer numOfDownloads;

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
