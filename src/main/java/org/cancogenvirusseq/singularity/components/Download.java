/*
 * Copyright (c) 2021 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.cancogenvirusseq.singularity.components;

import static org.cancogenvirusseq.singularity.components.FilesArchive.DOWNLOAD_DIR;

import io.netty.buffer.PooledByteBufAllocator;
import java.io.BufferedOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.components.model.AnalysisDocument;
import org.cancogenvirusseq.singularity.components.model.BatchedDownloadPair;
import org.cancogenvirusseq.singularity.components.model.MuseErrorResponse;
import org.cancogenvirusseq.singularity.components.model.MuseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

@Slf4j
@Component
public class Download {

  private static final NettyDataBufferFactory DATA_BUFFER_FACTORY =
      new NettyDataBufferFactory(new PooledByteBufAllocator());

  private final String museHost;
  private final RetryBackoffSpec clientsRetrySpec;
  private final Integer batchSize;
  private final Integer concurrentRequests;

  public Download(
      @Value("${download.museHost}") String museHost,
      @Value("${download.retryMaxAttempts}") Integer retryMaxAttempts,
      @Value("${download.retryDelaySec}") Integer retryDelaySec,
      @Value("${download.batchSize}") Integer batchSize,
      @Value("${download.concurrentRequests}") Integer concurrentRequests) {
    this.museHost = museHost;

    this.clientsRetrySpec =
        Retry.fixedDelay(retryMaxAttempts, Duration.ofSeconds(retryDelaySec))
            // Retry on non 5xx errors, 4xx is bad request no point retrying
            .filter(
                t ->
                    t instanceof MuseException
                        && ((MuseException) t).getStatus().is5xxServerError())
            .onRetryExhaustedThrow(((retryBackoffSpec, retrySignal) -> retrySignal.failure()));

    this.batchSize = batchSize;
    this.concurrentRequests = concurrentRequests;
  }

  public static String getDownloadPathForFileBundle(String filename) {
    return String.format("%s/%s", DOWNLOAD_DIR, filename);
  }

  private final BiFunction<FilesArchive, BatchedDownloadPair, FilesArchive> addToFileBundle =
      (filesArchive, batchedDownloadPair) -> {
        writeToFileStream(
            filesArchive.getMolecularFileOutputStream(),
            dataBufferToBytes(batchedDownloadPair.getMolecularData()));
        writeToFileStream(
            filesArchive.getMetadataFileOutputStream(),
            TsvWriter.analysisDocumentsToTsvRowsBytes(batchedDownloadPair.getAnalysisDocuments()));
        return filesArchive;
      };

  public Function<Flux<AnalysisDocument>, Flux<String>> downloadAndArchiveFunctionWithInstant(
      Instant instant) {
    return analysisDocs ->
        analysisDocs
            .buffer(batchSize)
            .flatMap(
                batchedAnalyses -> Flux.concat(downloadFromMuse(batchedAnalyses)),
                concurrentRequests)
            .reduce(new FilesArchive(instant), addToFileBundle)
            .map(FilesArchive.tarGzipBundleAndClose)
            .flux()
            .log("Download::downloadAndArchiveFunctionWithInstant");
  }

  private Flux<BatchedDownloadPair> downloadFromMuse(List<AnalysisDocument> analysisDocuments) {
    return WebClient.create()
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .scheme("https")
                    .host(museHost)
                    .path("download")
                    .queryParam(
                        "objectIds",
                        analysisDocuments.stream()
                            .map(AnalysisDocument::getObjectId)
                            .collect(Collectors.joining(",")))
                    .build())
        .exchangeToFlux(
            clientResponse ->
                clientResponse.statusCode().is2xxSuccessful()
                    ? clientResponse.bodyToFlux(NettyDataBuffer.class).concatWith(newLineBuffer())
                    : clientResponse
                        .bodyToMono(MuseErrorResponse.class)
                        .flux()
                        .flatMap(res -> Mono.error(new MuseException(res))))
        .transform(DataBufferUtils::join) // collect dataBuffers into single dataBuffer
        .map(dataBuffer -> new BatchedDownloadPair(analysisDocuments, dataBuffer))
        .log("Download::downloadFromMuse")
        .retryWhen(clientsRetrySpec);
  }

  private static Flux<NettyDataBuffer> newLineBuffer() {
    return Flux.just(newLineBufferSupplier.get().write("\n".getBytes(StandardCharsets.UTF_8)));
  }

  private static final Supplier<NettyDataBuffer> newLineBufferSupplier =
      () -> DATA_BUFFER_FACTORY.allocateBuffer(4);

  @SneakyThrows
  private void writeToFileStream(BufferedOutputStream stream, byte[] bytes) {
    stream.write(bytes);
  }

  private byte[] dataBufferToBytes(DataBuffer dataBuffer) {
    return Optional.of(new byte[dataBuffer.readableByteCount()])
        .map(
            bytes -> {
              dataBuffer.read(bytes);
              DataBufferUtils.release(dataBuffer);
              return bytes;
            })
        .orElseThrow();
  }
}
