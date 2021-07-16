package org.cancogenvirusseq.singularity.components;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.components.model.AnalysisDocument;
import org.cancogenvirusseq.singularity.components.model.AnalysisDocumentMolecularDataPair;
import org.cancogenvirusseq.singularity.config.s3Client.S3ClientProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Download implements Function<Flux<AnalysisDocument>, Flux<AnalysisDocumentMolecularDataPair>> {

    private final S3AsyncClient s3AsyncClient;
    private final S3ClientProperties s3ClientProperties;

    @Override
    public Flux<AnalysisDocumentMolecularDataPair> apply(Flux<AnalysisDocument> analysisDocumentFlux) {
        // todo: init object before?
        return analysisDocumentFlux.map(analysisDocument -> GetObjectRequest.builder().key(analysisDocument.getObjectId()).bucket(s3ClientProperties.getBucket()).build())
                .flatMap(getObjectRequest -> Mono.fromFuture(s3AsyncClient.getObject(getObjectRequest, AsyncResponseTransformer.toBytes())))
                .map(getObjectResponseResponseBytes -> new AnalysisDocumentMolecularDataPair(a) getObjectResponseResponseBytes.asByteBuffer().)
    }
}
