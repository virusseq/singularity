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
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.zip.GZIPOutputStream;

@Component
@RequiredArgsConstructor
public class Download {

  private final ScoreClient scoreClient;

  @SneakyThrows
  public Mono<GZIPOutputStream> getObjectIdsAsGZIPOutputStream(Flux<String> objectIds) {
    return objectIds
        .concatMap(objectId -> Flux.concat(scoreClient.downloadObject(objectId), newLineBuffer()))
        .reduce(
            new GZIPOutputStream(new DefaultDataBufferFactory().allocateBuffer().asOutputStream()),
            this::addToGzipStream)
        .map(this::closeStream);
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
