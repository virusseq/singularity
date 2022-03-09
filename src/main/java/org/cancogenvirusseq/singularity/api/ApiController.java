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

package org.cancogenvirusseq.singularity.api;

import static java.lang.String.format;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.api.model.EntityListResponse;
import org.cancogenvirusseq.singularity.api.model.ErrorResponse;
import org.cancogenvirusseq.singularity.api.model.SetIdBuildRequest;
import org.cancogenvirusseq.singularity.components.base.DownloadObjectById;
import org.cancogenvirusseq.singularity.components.model.TotalCounts;
import org.cancogenvirusseq.singularity.components.pipelines.Contributors;
import org.cancogenvirusseq.singularity.components.pipelines.SetQueryArchiveRequest;
import org.cancogenvirusseq.singularity.components.pipelines.TotalCountsPipeline;
import org.cancogenvirusseq.singularity.exceptions.http.ArchiveNotFoundHttpException;
import org.cancogenvirusseq.singularity.exceptions.http.BaseHttpException;
import org.cancogenvirusseq.singularity.exceptions.http.SetNotFoundHttpException;
import org.cancogenvirusseq.singularity.repository.ArchivesRepo;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.repository.query.FindArchivesQuery;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ApiController implements ApiDefinition {
  private final TotalCountsPipeline totalCountsPipeline;
  private final Contributors contributors;
  private final DownloadObjectById downloadObjectById;
  private final SetQueryArchiveRequest setQueryArchiveRequest;
  private final ArchivesRepo archivesRepo;

  @Override
  public Mono<EntityListResponse<String>> getContributors() {
    return contributors.getContributors().transform(this::listResponseTransform);
  }

  @Override
  public Mono<ResponseEntity<TotalCounts>> getTotalCounts() {
    return totalCountsPipeline
        .getTotalCounts()
        .map(ResponseEntity::ok)
        .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()));
  }

  @Override
  public Mono<ResponseEntity<Flux<ByteBuffer>>> downloadLatestAllArchive() {
    return archivesRepo.findLatestAllArchive().transform(this::processArchiveDownloadRequest);
  }

  @Override
  public Mono<ResponseEntity<Flux<ByteBuffer>>> downloadArchiveById(UUID id) {
    return archivesRepo.findCompletedArchiveById(id).transform(this::processArchiveDownloadRequest);
  }

  @Override
  public Mono<Page<Archive>> getArchives(FindArchivesQuery findArchivesQuery) {
    return archivesRepo.findByCommand(findArchivesQuery);
  }

  @Override
  public Mono<Archive> getArchive(UUID id) {
    return archivesRepo.findById(id).switchIfEmpty(Mono.error(new ArchiveNotFoundHttpException()));
  }

  @Override
  public Mono<Archive> buildArchiveWithSetId(SetIdBuildRequest setIdBuildRequest) {
    return setQueryArchiveRequest
        .apply(setIdBuildRequest.getSetId())
        .switchIfEmpty(Mono.error(new SetNotFoundHttpException()));
  }

  private <T> Mono<EntityListResponse<T>> listResponseTransform(
      Mono<? extends Collection<T>> entities) {
    return entities.map(entityList -> EntityListResponse.<T>builder().data(entityList).build());
  }

  private Mono<ResponseEntity<Flux<ByteBuffer>>> processArchiveDownloadRequest(
      Mono<Archive> archiveMono) {
    return archiveMono
        .map(Archive::incrementDownloadsForArchive)
        .flatMap(archivesRepo::save)
        .flatMap(
            archive ->
                downloadObjectById
                    .apply(archive.getObjectId())
                    .map(
                        archiveDownload ->
                            ResponseEntity.ok()
                                .header(
                                    HttpHeaders.CONTENT_DISPOSITION,
                                    format(
                                        "attachment; filename=%s",
                                        Archive.parseFilenameFromArchive(archive)))
                                .header(
                                    HttpHeaders.CONTENT_TYPE,
                                    MediaType.APPLICATION_OCTET_STREAM_VALUE)
                                .body(archiveDownload.getFlux())))
        .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
  }

  @ExceptionHandler
  public ResponseEntity<ErrorResponse> handle(Throwable ex) {
    log.error("ApiController exception handler", ex);
    if (ex instanceof BaseHttpException) {
      return ErrorResponse.errorResponseEntity((BaseHttpException) ex);
    } else {
      return ErrorResponse.errorResponseEntity(
          HttpStatus.INTERNAL_SERVER_ERROR, ex.getLocalizedMessage());
    }
  }
}
