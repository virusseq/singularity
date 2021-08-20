package org.cancogenvirusseq.singularity.exceptions.runtime;

import static java.lang.String.format;

import java.util.Optional;
import software.amazon.awssdk.core.SdkResponse;

public class S3DownloadFailedException extends RuntimeException {

  public S3DownloadFailedException(SdkResponse response) {
    super(
        Optional.ofNullable(response.sdkHttpResponse())
            .map(
                sdkHttpResponse ->
                    sdkHttpResponse
                        .statusText()
                        .map(
                            statusTextValue ->
                                format(
                                    "Download from object storage failed with status code: %d, status text: %s",
                                    sdkHttpResponse.statusCode(), statusTextValue))
                        .orElse(
                            format(
                                "Download from object storage failed with status code: %d, no status text provided",
                                sdkHttpResponse.statusCode())))
            .orElse("Unknown error has occurred!"));
  }
}
