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

package org.cancogenvirusseq.singularity.components.pipelines;

import static org.cancogenvirusseq.singularity.components.utils.FileArchiveUtils.deleteArchiveForInstant;

import java.time.Instant;
import java.util.function.Function;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.components.base.InstantToArchiveBuildRequest;
import org.cancogenvirusseq.singularity.components.events.EventEmitter;
import org.cancogenvirusseq.singularity.components.hoc.ArchiveBuildRequestToArchive;
import org.cancogenvirusseq.singularity.components.model.ArchiveBuildRequest;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllArchiveBuild {
  private final EventEmitter eventEmitter;
  private final InstantToArchiveBuildRequest instantToArchiveBuildRequest;
  private final ArchiveBuildRequestToArchive archiveBuildRequestToArchive;

  @Getter private Disposable allArchiveDisposable;
  @Getter private Disposable buildAllArchiveDisposable;

  @PostConstruct
  public void init() {
    // build file archive on app start
    buildAllArchiveDisposable = createBuildAllArchiveDisposable(Instant.now());

    // start file bundle update disposable
    allArchiveDisposable = createAllArchiveDisposable();
  }

  private Disposable createAllArchiveDisposable() {
    return eventEmitter
        .receive()
        .doOnNext(
            instant -> {
              log.debug("createAllArchiveDisposable received instant: {}", instant);

              if (!buildAllArchiveDisposable.isDisposed()) {
                log.debug("Existing archive build detected!");
                this.buildAllArchiveDisposable.dispose();
              }

              log.debug("Spawning new archive build...");
              this.buildAllArchiveDisposable = createBuildAllArchiveDisposable(instant);
            })
        .log("Files::createAllArchiveDisposable")
        .subscribe();
  }

  private Disposable createBuildAllArchiveDisposable(Instant instant) {
    return instantToArchiveBuildRequest
        .apply(instant)
        .flatMapMany(archiveBuildRequestToArchive)
        .doFinally(signalType -> deleteArchiveForInstant.accept(instant))
        .subscribe();
  }

  private <R> Mono<R> withArchiveBuildRequestContext(Function<ArchiveBuildRequest, Mono<R>> func) {
    return Mono.deferContextual(ctx -> func.apply(ctx.get("archiveBuildRequest")));
  }
}
