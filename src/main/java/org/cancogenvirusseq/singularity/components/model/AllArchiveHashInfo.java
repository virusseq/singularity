package org.cancogenvirusseq.singularity.components.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.checkerframework.checker.units.qual.A;

@Getter
@RequiredArgsConstructor
public class AllArchiveHashInfo {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final Long numSamples;
  private final String lastUpdated;

  public static AllArchiveHashInfo parseFromCountAndLastUpdatedResult(
      CountAndLastUpdatedResult countAndLastUpdatedResult) {
    return new AllArchiveHashInfo(
        countAndLastUpdatedResult.getNumDocuments().getValue(),
        countAndLastUpdatedResult.getLastUpdatedDate().getValueAsString());
  }

  @Override
  @SneakyThrows
  public String toString() {
    return AllArchiveHashInfo.objectMapper.writeValueAsString(this);
  }
}
