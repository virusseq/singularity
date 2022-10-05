package org.cancogenvirusseq.singularity.exceptions.runtime;

import static java.lang.String.format;

import org.cancogenvirusseq.singularity.repository.model.Archive;

public class ExistingArchiveRestartException extends RuntimeException {
  public ExistingArchiveRestartException(Archive existingArchive) {
    super(
        format(
            "There is an existing archive with hash matching {}, created at {} that is in a correct state: {}. It should not be restarted.",
            existingArchive.getHash(),
            existingArchive.getCreatedAt(),
            existingArchive.getStatus()));
  }
}
