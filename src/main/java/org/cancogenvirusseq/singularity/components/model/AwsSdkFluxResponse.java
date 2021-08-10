package org.cancogenvirusseq.singularity.components.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

@Data
@NoArgsConstructor
public class AwsSdkFluxResponse {
  private final CompletableFuture<AwsSdkFluxResponse> cf = new CompletableFuture<>();
  private GetObjectResponse sdkResponse;
  private Flux<ByteBuffer> flux;
}
