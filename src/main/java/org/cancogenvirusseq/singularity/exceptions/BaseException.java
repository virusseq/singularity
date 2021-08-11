package org.cancogenvirusseq.singularity.exceptions;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

public interface BaseException {
  default HttpStatus getStatusCode() {
    return HttpStatus.BAD_REQUEST;
  }

  String getMessage();

  default Map<String, Object> getErrorInfo() {
    return new HashMap<>();
  }
}
