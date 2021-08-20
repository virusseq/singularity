package org.cancogenvirusseq.singularity.exceptions.http;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

public interface BaseHttpException {
  default HttpStatus getStatusCode() {
    return HttpStatus.BAD_REQUEST;
  }

  String getMessage();

  default Map<String, Object> getErrorInfo() {
    return new HashMap<>();
  }
}
