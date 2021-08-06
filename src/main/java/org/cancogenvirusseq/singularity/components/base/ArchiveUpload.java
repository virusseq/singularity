package org.cancogenvirusseq.singularity.components.base;

import static java.lang.String.format;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.config.s3Client.S3ClientProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveUpload implements Function<Path, Mono<Path>> {
  private final S3AsyncClient s3AsyncClient;
  private final S3ClientProperties s3ClientProperties;

  @Override
  public Mono<Path> apply(Path archivePath) {
    return Mono.fromFuture(
            s3AsyncClient.putObject(
                putObjectRequestForArchive(archivePath), AsyncRequestBody.fromFile(archivePath)))
        .map(
            result -> {
              if (result.sdkHttpResponse() == null || !result.sdkHttpResponse().isSuccessful()) {
                throw new RuntimeException(result.toString());
              }

              log.debug("Successfully uploaded archive: {}", archivePath.getFileName());

              return archivePath;
            })
        .log("ArchiveUpload");
  }

  @SneakyThrows
  private PutObjectRequest putObjectRequestForArchive(Path archivePath) {
    return PutObjectRequest.builder()
        .key(format("%s/%s", s3ClientProperties.getDataDir(), UUID.randomUUID()))
        .bucket(s3ClientProperties.getBucket())
        .contentLength(Files.size(archivePath))
        .metadata(new HashMap<String, String>())
        .build();
  }
}
