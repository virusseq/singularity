package org.cancogenvirusseq.singularity.components.base;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.config.s3Client.S3ClientProperties;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.String.format;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveUpload implements Function<String, Mono<String>> {
  private final S3AsyncClient s3AsyncClient;
  private final S3ClientProperties s3ClientProperties;

  @Override
  public Mono<String> apply(String archiveFilename) {
    return Mono.fromFuture(
            s3AsyncClient.putObject(
                putObjectRequestForArchive(archiveFilename),
                AsyncRequestBody.fromFile(Paths.get(archiveFilename))))
        .map(
            result -> {
              if (result.sdkHttpResponse() == null || !result.sdkHttpResponse().isSuccessful()) {
                throw new RuntimeException(result.toString());
              }

              log.debug("Successfully updated archive: {}", archiveFilename);

              return archiveFilename;
            })
        .log("ArchiveUpload");
  }

  @SneakyThrows
  private PutObjectRequest putObjectRequestForArchive(String archiveFilename) {
    return PutObjectRequest.builder()
        .key(format("%s/%s", s3ClientProperties.getDataDir(), UUID.randomUUID()))
        .bucket(s3ClientProperties.getBucket())
        .contentLength(Files.size(Paths.get(archiveFilename)))
        .metadata(new HashMap<String, String>())
        .build();
  }
}
