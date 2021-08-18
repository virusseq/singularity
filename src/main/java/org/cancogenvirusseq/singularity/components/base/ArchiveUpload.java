package org.cancogenvirusseq.singularity.components.base;

import static java.lang.String.format;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.config.s3Client.S3ClientProperties;
import org.cancogenvirusseq.singularity.exceptions.runtime.S3ArchiveUploadException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveUpload implements Function<Path, Mono<String>> {
  private final String ARCHIVE_MEDIA_TYPE = "application/x-gtar";

  private final S3Presigner s3Presigner;
  private final S3ClientProperties s3ClientProperties;

  @Override
  public Mono<String> apply(Path fileBundlePath) {
    return HttpClient.create()
        .headers(
            h -> {
              h.set(HttpHeaderNames.CONTENT_LENGTH, getFileSize(fileBundlePath));
              h.set(HttpHeaderNames.CONTENT_TYPE, ARCHIVE_MEDIA_TYPE);
            })
        .put()
        .uri(getPresignedUrlStringForFileBundle(fileBundlePath))
        .send(ByteBufFlux.fromPath(fileBundlePath))
        .response()
        .map(getResponseFunctionForPath(fileBundlePath))
        .log("ArchiveUpload");
  }

  private final BiFunction<S3ClientProperties, Path, PutObjectRequest> createPutObjectRequest =
      (s3ClientProperties, fileBundlePath) ->
          PutObjectRequest.builder()
              .key(format("%s/%s", s3ClientProperties.getDataDir(), UUID.randomUUID()))
              .bucket(s3ClientProperties.getBucket())
              .contentLength(getFileSize(fileBundlePath))
              .contentType(ARCHIVE_MEDIA_TYPE)
              .build();

  private final Function<PutObjectRequest, PutObjectPresignRequest> createPutObjectPresignRequest =
      putObjectRequest ->
          PutObjectPresignRequest.builder()
              .signatureDuration(Duration.ofMinutes(10))
              .putObjectRequest(putObjectRequest)
              .build();

  private final BiFunction<S3Presigner, PutObjectPresignRequest, PresignedPutObjectRequest>
      createPresignedPutObjectRequest = S3Presigner::presignPutObject;

  private String getPresignedUrlStringForFileBundle(Path fileBundlePath) {
    return createPutObjectRequest
        .andThen(createPutObjectPresignRequest)
        .andThen(
            putObjectPreSignRequest ->
                createPresignedPutObjectRequest.apply(s3Presigner, putObjectPreSignRequest))
        .andThen(PresignedRequest::url)
        .andThen(URL::toString)
        .apply(s3ClientProperties, fileBundlePath);
  }

  @SneakyThrows
  private Long getFileSize(Path path) {
    return Files.size(path);
  }

  private Function<HttpClientResponse, String> getResponseFunctionForPath(Path fileBundlePath) {
    return response -> {
      if (response.status() != HttpResponseStatus.OK) {
        throw new S3ArchiveUploadException(response);
      }

      log.debug(
          "Successfully uploaded archive: {} to s3 path: {}",
          fileBundlePath.getFileName(),
          response.path());

      // return the objectId only
      return getObjectIdFromResponse(response);
    };
  }

  private String getObjectIdFromResponse(HttpClientResponse response) {
    return response.path().substring(response.path().lastIndexOf("/") + 1);
  }
}
