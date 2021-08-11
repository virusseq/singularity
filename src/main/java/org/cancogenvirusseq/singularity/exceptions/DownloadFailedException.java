package org.cancogenvirusseq.singularity.exceptions;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.http.SdkHttpResponse;

@AllArgsConstructor
public class DownloadFailedException extends RuntimeException implements BaseException {

  private int statusCode;
  private Optional<String> statusText;

  public DownloadFailedException(SdkResponse response) {

    SdkHttpResponse httpResponse = response.sdkHttpResponse();
    if (httpResponse != null) {
      this.statusCode = httpResponse.statusCode();
      this.statusText = httpResponse.statusText();
    } else {
      this.statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
      this.statusText = Optional.of("UNKNOWN");
    }
  }
}
