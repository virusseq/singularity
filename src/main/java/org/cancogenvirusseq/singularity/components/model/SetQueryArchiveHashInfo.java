package org.cancogenvirusseq.singularity.components.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Getter
@RequiredArgsConstructor
public class SetQueryArchiveHashInfo {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final Map<String, Object> sqon;
  private final Integer numSamples;
  private final String lastUpdated;

  @Override
  @SneakyThrows
  public String toString() {
    return SetQueryArchiveHashInfo.objectMapper.writeValueAsString(this);
  }
}
