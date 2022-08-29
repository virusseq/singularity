package org.cancogenvirusseq.singularity.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Summary{
  private int success;
  private int error;
  private int ignored;
}
