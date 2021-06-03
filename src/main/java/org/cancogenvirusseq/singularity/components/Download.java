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

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.components.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

@Slf4j
@Component
public class Download {

  private final String museHost;
  private final RetryBackoffSpec clientsRetrySpec;
  private final Integer batchSize;
  private final Integer concurrentRequests;

  private static final String DOWNLOAD_DIR = "/tmp";
  private static final String FILE_NAME_TEMPLATE = "virusseq-consensus-export-all-";
  public static final String FASTA_FILE_EXTENSION = ".fasta";

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

  private final BiFunction<FileBundle, BatchedDownloadPair, FileBundle> addToFileBundle =
      (fileBundle, batchedDownloadPair) -> {
        writeToFileStream(
            fileBundle.getMolecularFile(),
            dataBufferToBytes(batchedDownloadPair.getMolecularData()));
        writeToFileStream(
            fileBundle.getMetadataFile(),
            TsvWriter.analysisDocumentsToTsvRowsBytes(batchedDownloadPair.getAnalysisDocuments()));
        return fileBundle;
      };

  public Function<Flux<AnalysisDocument>, Flux<String>> downloadGzipFunctionWithInstant(
      Instant instant) {
    return analysisDocs ->
        analysisDocs
            .buffer(batchSize)
            .flatMap(
                batchedAnalyses -> Flux.concat(downloadFromMuse(batchedAnalyses)),
                concurrentRequests)
            .reduce(new FileBundle(instant), addToFileBundle)
            .map(
                fileBundle -> {
                  fileBundle.closeFileStreams();
                  return fileBundle.getDirectory();
                })
            .flux();
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
                    ? clientResponse.bodyToFlux(DataBuffer.class).concatWith(newLineBuffer())
                    : clientResponse
                        .bodyToMono(MuseErrorResponse.class)
                        .flux()
                        .flatMap(res -> Mono.error(new MuseException(res))))
        .map(dataBuffer -> new BatchedDownloadPair(analysisDocuments, dataBuffer))
        .log("Download::downloadFromMuse", Level.FINE)
        .retryWhen(clientsRetrySpec);
  }

  private static Flux<DataBuffer> newLineBuffer() {
    return Flux.just(newLineBufferSupplier.get().write("\n".getBytes(StandardCharsets.UTF_8)));
  }

  private static final Supplier<DefaultDataBuffer> newLineBufferSupplier =
      () -> new DefaultDataBufferFactory().allocateBuffer(4);

  @SneakyThrows
  private void writeToFileStream(FileOutputStream stream, byte[] bytes) {
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
