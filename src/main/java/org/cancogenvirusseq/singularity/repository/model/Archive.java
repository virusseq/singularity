package org.cancogenvirusseq.singularity.repository.model;

import static java.lang.String.format;

import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.cancogenvirusseq.singularity.components.model.AllArchiveHashInfo;
import org.cancogenvirusseq.singularity.components.model.CountAndLastUpdatedResult;
import org.cancogenvirusseq.singularity.components.model.SetQueryArchiveHashInfo;
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

  @NonNull private Long numOfSamples;

  // always initialized to zero when creating
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

  public static Archive newAllArchiveFromCountAndLastUpdatedResult(
      CountAndLastUpdatedResult countAndLastUpdatedResult) {
    return Archive.builder()
        .status(ArchiveStatus.BUILDING)
        .type(ArchiveType.ALL)
        .hashInfo(
            AllArchiveHashInfo.parseFromCountAndLastUpdatedResult(countAndLastUpdatedResult)
                .toString())
        .numOfSamples(countAndLastUpdatedResult.getNumDocuments().getValue())
        .build();
  }

  public static Archive newFromSetQueryArchiveHashInfo(
      SetQueryArchiveHashInfo setQueryArchiveHashInfo) {
    return Archive.builder()
        .status(ArchiveStatus.BUILDING)
        .type(ArchiveType.SET_QUERY)
        .hashInfo(setQueryArchiveHashInfo.toString())
        .numOfSamples(setQueryArchiveHashInfo.getNumSamples())
        .build();
  }

  public static Archive incrementDownloadsForArchive(Archive archive) {
    archive.setNumOfDownloads(archive.getNumOfDownloads() + 1);
    return archive;
  }

  public static String parseFilenameFromArchive(Archive archive) {
    if (archive.getType().equals(ArchiveType.ALL)) {
      // for a download all entry, use the createdAt timestamp for the filename
      return format(
              "virusseq-data-release-%s.tar.gz", Instant.ofEpochSecond(archive.getCreatedAt()));
    } else {
      // for all other export types just note that it's an export and the download time
      return format("virusseq-search-export-%s.gz", Instant.now());
    }
  }
}
