package org.cancogenvirusseq.singularity.exceptions;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.http.HttpStatus;

@Value
@EqualsAndHashCode(callSuper = true)
public class ArchiveNotFoundException extends Throwable implements BaseException {
  @Override
  public HttpStatus getStatusCode() {
    return HttpStatus.NOT_FOUND;
  }

  @Override
  public String getMessage() {
    return "The archive you are looking for does not exist";
  }
}
