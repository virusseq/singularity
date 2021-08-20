package org.cancogenvirusseq.singularity.exceptions.runtime;

import static java.lang.String.format;

import reactor.netty.http.client.HttpClientResponse;

public class S3ArchiveUploadException extends RuntimeException {
  public S3ArchiveUploadException(HttpClientResponse response) {
    super(
        format(
            "ArchiveUpload failed to send archive to object storage with response: %s", response));
  }
}
