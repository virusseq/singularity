package org.cancogenvirusseq.singularity.repository.model;

import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class ArchiveTypeConverter implements Converter<ArchiveType, ArchiveType> {
  @Override
  public ArchiveType convert(@NonNull ArchiveType source) {
    return source;
  }
}
