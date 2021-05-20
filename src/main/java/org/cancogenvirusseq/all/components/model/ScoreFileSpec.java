package org.cancogenvirusseq.all.components.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ScoreFileSpec {
  private String objectId;
  private String uploadId;
  private List<Part> parts;

  @Data
  @NoArgsConstructor
  public static class Part {
    Integer partNumber;
    Long partSize;
    Long offset;
    String url;
  }
}
