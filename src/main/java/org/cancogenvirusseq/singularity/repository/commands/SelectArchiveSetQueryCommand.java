package org.cancogenvirusseq.singularity.repository.commands;

import java.util.Optional;
import java.util.UUID;
import lombok.*;
import org.cancogenvirusseq.singularity.repository.model.ArchiveAll;
import org.cancogenvirusseq.singularity.repository.model.ArchiveStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SelectArchiveSetQueryCommand {
  UUID id;
  ArchiveStatus status;
  String setQueryHash;
  @NonNull Integer offset = 0;
  @NonNull Integer size = 20;
  @NonNull Sort.Direction sortDirection = Sort.Direction.ASC;
  @NonNull ArchiveAll.Fields sortField = ArchiveAll.Fields.timestamp;

  public Optional<UUID> getId() {
    return Optional.ofNullable(id);
  }

  public Optional<ArchiveStatus> getStatus() {
    return Optional.ofNullable(status);
  }

  public Optional<String> getSetQueryHash() {
    return Optional.ofNullable(setQueryHash);
  }

  public Pageable cretePageable() {
    return PageRequest.of(
        getPage(), getSize(), Sort.by(getSortDirection(), getSortField().toString()));
  }

  private Integer getPage() {
    return offset / size;
  }
}
