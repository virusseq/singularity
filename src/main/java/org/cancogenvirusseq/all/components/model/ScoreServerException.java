package org.cancogenvirusseq.all.components.model;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.http.HttpStatus;

import static java.lang.String.format;

@Value
@EqualsAndHashCode(callSuper = true)
public class ScoreServerException extends Throwable {
  HttpStatus status;
  String message;

  public String toString() {
    return format("%s - %s", status.toString(), message);
  }
}
