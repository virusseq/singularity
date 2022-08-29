package org.cancogenvirusseq.singularity.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HashResult {
  private String hash;
  private String oldStatus;
  private String newStatus;
  private String createdAt;
  private String type;
  private long numberOfSamples;
}
