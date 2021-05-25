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

package org.cancogenvirusseq.all.components;

import com.google.common.io.ByteStreams;
import java.io.FileOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;
import lombok.SneakyThrows;
import lombok.val;
import org.cancogenvirusseq.all.components.model.MuseErrorResponse;
import org.cancogenvirusseq.all.components.model.MuseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

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

  public Function<Flux<String>, Flux<String>> downloadGzipFunctionWithInstant(Instant instant) {
    return objectIds ->
        objectIds
            .buffer(batchSize)
            .flatMap(
                batchedIds -> Flux.concat(downloadFromMuse(batchedIds), newLineBuffer()),
                concurrentRequests)
            .reduce(makeGzipOutputStream(instant), this::addToGzipStream)
            .map(this::closeStream)
            .then(
                Mono.just(
                    String.format(
                        "%s%s%s.gz", FILE_NAME_TEMPLATE, instant.toString(), FASTA_FILE_EXTENSION)))
            .flux();
  }

  public static String getDownloadPathForFileBundle(String filename) {
    return String.format("%s/%s", DOWNLOAD_DIR, filename);
  }

  @SneakyThrows
  private GZIPOutputStream makeGzipOutputStream(Instant instant) {
    return new GZIPOutputStream(
        new FileOutputStream(
            String.format(
                "%s/%s%s%s.gz",
                DOWNLOAD_DIR, FILE_NAME_TEMPLATE, instant.toString(), FASTA_FILE_EXTENSION)));
  }

  private Flux<DataBuffer> downloadFromMuse(List<String> objectIds) {
    return WebClient.create()
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .scheme("https")
                    .host(museHost)
                    .path("download")
                    .queryParam("objectIds", String.join(",", objectIds))
                    .build())
        .exchangeToFlux(
            clientResponse ->
                clientResponse.statusCode().is2xxSuccessful()
                    ? clientResponse.bodyToFlux(DataBuffer.class)
                    : clientResponse
                        .bodyToMono(MuseErrorResponse.class)
                        .flux()
                        .flatMap(res -> Mono.error(new MuseException(res))))
        .log("Download::downloadFromMuse", Level.FINE)
        .retryWhen(clientsRetrySpec);
  }

  @SneakyThrows
  private GZIPOutputStream addToGzipStream(GZIPOutputStream gzip, DataBuffer inputDataBuffer) {
    val inputDataBufferStream = inputDataBuffer.asInputStream();
    val bytes = ByteStreams.toByteArray(inputDataBufferStream);
    gzip.write(bytes);
    inputDataBufferStream.close();
    return gzip;
  }

  @SneakyThrows
  private GZIPOutputStream closeStream(GZIPOutputStream gzip) {
    gzip.close();
    return gzip;
  }

  private static Flux<DataBuffer> newLineBuffer() {
    val buffer = new DefaultDataBufferFactory().allocateBuffer(4);
    val newLIne = "\n";
    buffer.write(newLIne.getBytes());
    return Flux.just(buffer);
  }
}
