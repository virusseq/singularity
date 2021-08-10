package org.cancogenvirusseq.singularity.utils;

import org.cancogenvirusseq.singularity.components.model.AwsSdkFluxResponse;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class FluxResponseProvider implements AsyncResponseTransformer<GetObjectResponse, AwsSdkFluxResponse> {
  private AwsSdkFluxResponse response;

  @Override
  public CompletableFuture<AwsSdkFluxResponse> prepare() {
    response = new AwsSdkFluxResponse();
    return response.getCf();
  }

  @Override
  public void onResponse(GetObjectResponse sdkResponse) {
    response.setSdkResponse(sdkResponse);
  }

  @Override
  public void onStream(SdkPublisher<ByteBuffer> publisher) {
    response.setFlux(Flux.from(publisher));
    response.getCf().complete(response);
  }

  @Override
  public void exceptionOccurred(Throwable error) {
    response.getCf().completeExceptionally(error);
  }
}
