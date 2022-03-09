package org.cancogenvirusseq.singularity.components.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TotalCounts {
  Long files;
  Long samples;
  Integer studies;
  Long fileSizeBytes;
  String fileSizeHumanReadable;
  Long timestamp;
}
