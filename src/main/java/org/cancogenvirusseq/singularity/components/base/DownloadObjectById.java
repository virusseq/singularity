package org.cancogenvirusseq.singularity.components.base;

import static java.lang.String.format;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.components.model.AwsSdkFluxResponse;
import org.cancogenvirusseq.singularity.config.s3Client.S3ClientProperties;
import org.cancogenvirusseq.singularity.exceptions.DownloadFailedException;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.utils.FluxResponseProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class DownloadObjectById implements Function<UUID, Mono<AwsSdkFluxResponse>> {
  private final S3AsyncClient s3AsyncClient;
  private final S3ClientProperties s3ClientProperties;

  @Override
  public Mono<AwsSdkFluxResponse> apply(UUID objectId) {
    return Mono.fromFuture(
            s3AsyncClient.getObject(
                getObjectRequestForObjectId(objectId), new FluxResponseProvider()))
        .map(verifyResponse);
  }

  public Mono<AwsSdkFluxResponse> apply(Archive archive) {
    return this.apply(archive.getObjectId());
  }

  private GetObjectRequest getObjectRequestForObjectId(UUID objectId) {
    return GetObjectRequest.builder()
        .key(format("%s/%s", s3ClientProperties.getDataDir(), objectId))
        .bucket(s3ClientProperties.getBucket())
        .build();
  }

  private final Predicate<SdkHttpResponse> responseNotNull = Objects::nonNull;

  private final Predicate<SdkHttpResponse> responseIsSuccessful = SdkHttpResponse::isSuccessful;

  private final UnaryOperator<AwsSdkFluxResponse> verifyResponse =
      response -> {
        if (responseNotNull
            .and(responseIsSuccessful)
            .test(response.getSdkResponse().sdkHttpResponse())) {
          return response;
        }

        throw new DownloadFailedException(response.getSdkResponse());
      };
}
