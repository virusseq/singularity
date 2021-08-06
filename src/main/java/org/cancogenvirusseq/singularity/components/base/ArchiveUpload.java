package org.cancogenvirusseq.singularity.components.base;

import static java.lang.String.format;

import java.nio.file.Files;
import java.nio.file.Path;
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
import software.amazon.awssdk.utils.Md5Utils;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveUpload implements Function<Path, Mono<String>> {
  private final S3AsyncClient s3AsyncClient;
  private final S3ClientProperties s3ClientProperties;

  @Override
  public Mono<String> apply(Path filesArchivePath) {
    return Mono.fromFuture(
            s3AsyncClient.putObject(
                putObjectRequestForArchive(filesArchivePath),
                AsyncRequestBody.fromFile(filesArchivePath)))
        .map(
            result -> {
              if (result.sdkHttpResponse() == null || !result.sdkHttpResponse().isSuccessful()) {
                throw new RuntimeException(result.toString());
              }

              log.debug("Successfully uploaded archive: {}", filesArchivePath.getFileName());

              return filesArchivePath.getFileName().toString();
            })
        .log("ArchiveUpload");
  }

  @SneakyThrows
  private PutObjectRequest putObjectRequestForArchive(Path filesArchivePath) {
    return PutObjectRequest.builder()
        .key(format("%s/%s", s3ClientProperties.getDataDir(), UUID.randomUUID()))
        .bucket(s3ClientProperties.getBucket())
        .contentMD5(Md5Utils.md5AsBase64(Files.readAllBytes(filesArchivePath)))
        .build();
  }
}
