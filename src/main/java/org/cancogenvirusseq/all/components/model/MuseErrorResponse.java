package org.cancogenvirusseq.all.components.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MuseErrorResponse {
  private HttpStatus status;
  private String message;
  private Map<String, Object> errorInfo;
}
