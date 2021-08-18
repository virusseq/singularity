package org.cancogenvirusseq.singularity.repository.model;

import static java.lang.String.format;

import java.util.UUID;
import lombok.*;
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

  @NonNull private Integer numOfSamples;

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
      // for a download all entry the hash info is the instant string
      return format("virusseq-consensus-archive-all-%s.tar.gz", archive.getHashInfo());
    } else {
      // otherwise just note the archiveId with the download
      return format("virusseq-consensus-archive-%s.tar.gz", archive.getId());
    }
  }
}
