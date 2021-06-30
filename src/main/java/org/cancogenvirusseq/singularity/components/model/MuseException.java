package org.cancogenvirusseq.singularity.components.model;

import static java.lang.String.format;

import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

@Data
@EqualsAndHashCode(callSuper = true)
public class MuseException extends Throwable {
  private final HttpStatus status;
  private final String message;
  private final Map<String, Object> errorInfo;

  public MuseException(MuseErrorResponse errorResponse) {
    this.status = errorResponse.getStatus();
    this.message = errorResponse.getMessage();
    this.errorInfo = errorResponse.getErrorInfo();
  }

  public String toString() {
    return format("%s - %s - %s", status.toString(), message, errorInfo.toString());
  }
}
