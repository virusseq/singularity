package org.cancogenvirusseq.singularity.components.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.util.UUID;

@Value
@Data
@Builder
@Table("total_count")
public class TotalCounts {
  @Id
  @JsonInclude(JsonInclude.Include.NON_NULL)
  UUID id;
  Long files;
  Long samples;
  Integer studies;
  Long fileSizeBytes;
  String fileSizeHumanReadable;
  Long timestamp;
}
