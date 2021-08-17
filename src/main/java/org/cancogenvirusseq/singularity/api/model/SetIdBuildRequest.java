package org.cancogenvirusseq.singularity.api.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class SetIdBuildRequest {
  private UUID setId;
}
