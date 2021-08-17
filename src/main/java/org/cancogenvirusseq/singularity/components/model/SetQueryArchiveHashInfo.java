package org.cancogenvirusseq.singularity.components.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SetQueryArchiveHashInfo {
  private final String sqon;
  @Getter private final Integer numSamples;
  private final String lastUpdated;

  // todo: toString for JSON output string here
}
