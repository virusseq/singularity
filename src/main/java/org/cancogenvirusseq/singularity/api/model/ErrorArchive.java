package org.cancogenvirusseq.singularity.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ErrorArchive{
  private String hash;
  private String status;
  private String errorMessage;
}
