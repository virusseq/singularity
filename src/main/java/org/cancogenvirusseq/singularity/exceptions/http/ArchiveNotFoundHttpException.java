package org.cancogenvirusseq.singularity.exceptions.http;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.http.HttpStatus;

@Value
@EqualsAndHashCode(callSuper = true)
public class ArchiveNotFoundHttpException extends Throwable implements BaseHttpException {
  @Override
  public HttpStatus getStatusCode() {
    return HttpStatus.NOT_FOUND;
  }

  @Override
  public String getMessage() {
    return "The archive you are looking for does not exist";
  }
}
