package org.cancogenvirusseq.all.components.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static java.lang.String.format;
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
