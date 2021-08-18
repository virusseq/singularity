package org.cancogenvirusseq.singularity.api.model;

import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SetIdBuildRequest {
  private UUID setId;
}
