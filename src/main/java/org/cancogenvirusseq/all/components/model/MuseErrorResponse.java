package org.cancogenvirusseq.all.components.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MuseErrorResponse {
  private HttpStatus status;
  private String message;
  private Map<String, Object> errorInfo;
}
