package org.cancogenvirusseq.singularity.repository.commands;

import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cancogenvirusseq.singularity.repository.model.ArchiveAll;
import org.cancogenvirusseq.singularity.repository.model.ArchiveStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectArchiveSetQueryCommand {
  UUID id;
  ArchiveStatus status;
  String setQueryHash;
  @Builder.Default Integer offset = 0;
  @Builder.Default Integer size = 20;
  @Builder.Default Sort.Direction sortDirection = Sort.Direction.ASC;
  @Builder.Default ArchiveAll.Fields sortField = ArchiveAll.Fields.timestamp;

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
