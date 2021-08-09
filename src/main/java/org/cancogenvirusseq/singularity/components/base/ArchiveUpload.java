package org.cancogenvirusseq.singularity.components.base;

import com.google.common.net.MediaType;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.config.s3Client.S3ClientProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;
import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Function;

import static java.lang.String.format;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveUpload implements Function<Path, Mono<String>> {
  private final S3Presigner s3Presigner;
  private final S3ClientProperties s3ClientProperties;

  private Function<Path, URL> getPresignedURLForFileArchive;

  @Override
  public Mono<String> apply(Path filesArchivePath) {
    return HttpClient.create()
        .headers(
            h -> {
              h.set(HttpHeaderNames.CONTENT_LENGTH, getFileSize(filesArchivePath));
              h.set(HttpHeaderNames.CONTENT_TYPE, MediaType.TAR);
            })
        .put()
        .uri(getPresignedURLForFileArchive.apply(filesArchivePath).toString())
        .send(ByteBufFlux.fromPath(filesArchivePath))
        .response()
        .map(
            result -> {
              if (result.status() != HttpResponseStatus.OK) {
                throw new RuntimeException(result.toString());
              }

              log.debug(
                  "Successfully uploaded archive: {} to s3 path: {}",
                  filesArchivePath.getFileName(),
                  result.path());

              // return the objectId only
              return result.path().substring(result.path().lastIndexOf("/") + 1);
            })
        .log("ArchiveUpload");
  }

  @PostConstruct
  public void init() {
    Function<Path, PutObjectRequest> createPutObjectRequest =
        filesArchivePath ->
            PutObjectRequest.builder()
                .key(format("%s/%s", s3ClientProperties.getDataDir(), UUID.randomUUID()))
                .bucket(s3ClientProperties.getBucket())
                .build();

    Function<PutObjectRequest, PutObjectPresignRequest> createPutObjectPresignRequest =
        putObjectRequest ->
            PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObjectRequest)
                .build();

    Function<PutObjectPresignRequest, PresignedPutObjectRequest> createPresignedPutObjectRequest =
        s3Presigner::presignPutObject;

    this.getPresignedURLForFileArchive =
        createPutObjectRequest
            .andThen(createPutObjectPresignRequest)
            .andThen(createPresignedPutObjectRequest)
            .andThen(PresignedRequest::url);
  }

  @SneakyThrows
  private Long getFileSize(Path path) {
    return Files.size(path);
  }
}
