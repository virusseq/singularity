package org.cancogenvirusseq.singularity.repository.model;

import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class ArchiveStatusConverter implements Converter<ArchiveStatus, ArchiveStatus> {
  @Override
  public ArchiveStatus convert(@NonNull ArchiveStatus source) {
    return source;
  }
}
