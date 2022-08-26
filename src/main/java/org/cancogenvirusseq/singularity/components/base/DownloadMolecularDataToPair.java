package org.cancogenvirusseq.singularity.components.base;

import static java.lang.String.format;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.components.model.AnalysisDocument;
import org.cancogenvirusseq.singularity.components.model.AnalysisDocumentMolecularDataPair;
import org.cancogenvirusseq.singularity.config.s3Client.S3ClientProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class DownloadMolecularDataToPair
    implements Function<Flux<AnalysisDocument>, Flux<AnalysisDocumentMolecularDataPair>> {

  private static final byte[] newlineBytes = "\n".getBytes(StandardCharsets.UTF_8);

  private final S3AsyncClient s3AsyncClient;
  private final S3ClientProperties s3ClientProperties;

  @Override
  public Flux<AnalysisDocumentMolecularDataPair> apply(
      Flux<AnalysisDocument> analysisDocumentFlux) {
    Hooks.onOperatorDebug();
    return analysisDocumentFlux
        .flatMap(
            analysisDocument ->
                // we can go straight to object storage and get the bytes as we know that these
                // objects are already verified to be there by song/score else they wouldn't be
                // in an AnalysisDocument, otherwise we would use the DownloadObjectById
                // component here
                Mono.fromFuture(
                        s3AsyncClient.getObject(
                            getObjectRequestForAnalysisDocument(analysisDocument),
                            AsyncResponseTransformer.toBytes())).checkpoint("Calling s3 getObject")
                    .map(
                        getObjectResponseResponseBytes ->
                            new AnalysisDocumentMolecularDataPair(
                                analysisDocument,
                                molecularDataBufferWithNewline(
                                    getObjectResponseResponseBytes.asByteArray()))).checkpoint("Building document pair").log("Building document pair log"),
            s3ClientProperties.getMaxConcurrency())
        .doOnError(
            throwable -> log.info("DownloadMolecularDataToPair" + throwable.getLocalizedMessage()));
  }

  private GetObjectRequest getObjectRequestForAnalysisDocument(AnalysisDocument analysisDocument) {
    return GetObjectRequest.builder()
        .key(format("%s/%s", s3ClientProperties.getDataDir(), analysisDocument.getObjectId().textValue()))
        .bucket(s3ClientProperties.getBucket())
        .build();
  }

  private byte[] molecularDataBufferWithNewline(byte[] molecularBytes) {
    return ByteBuffer.allocate(molecularBytes.length + newlineBytes.length)
        .put(molecularBytes)
        .put(newlineBytes)
        .array();
  }
}
