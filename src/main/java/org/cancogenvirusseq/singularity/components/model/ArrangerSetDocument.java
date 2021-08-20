package org.cancogenvirusseq.singularity.components.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArrangerSetDocument {
  @NonNull private UUID setId;
  @NonNull private Long createdAt;
  @NonNull private List<String> ids;
  @NonNull private Map<String, Object> sqon;
  @NonNull private Long size;
}
