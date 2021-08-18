package org.cancogenvirusseq.singularity.exceptions.http;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.http.HttpStatus;

@Value
@EqualsAndHashCode(callSuper = true)
public class SetNotFoundHttpException extends Throwable implements BaseHttpException {
  @Override
  public HttpStatus getStatusCode() {
    return HttpStatus.NOT_FOUND;
  }

  @Override
  public String getMessage() {
    return "The set you are requesting does not exist";
  }
}
