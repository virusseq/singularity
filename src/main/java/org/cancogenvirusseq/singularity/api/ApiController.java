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
import org.cancogenvirusseq.singularity.components.base.DownloadObjectById;
import org.cancogenvirusseq.singularity.pipelines.Contributors;
import org.cancogenvirusseq.singularity.repository.ArchivesRepo;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.repository.model.ArchiveType;
import org.cancogenvirusseq.singularity.repository.query.FindArchivesQuery;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ApiController implements ApiDefinition {
  private final Contributors contributors;
  private final DownloadObjectById downloadObjectById;
  private final ArchivesRepo archivesRepo;

  public Mono<EntityListResponse<String>> getContributors() {
    return contributors.getContributors().transform(this::listResponseTransform);
  }

  public Mono<ResponseEntity<Flux<ByteBuffer>>> downloadLatestAllArchive() {
    return archivesRepo.findLatestAllArchive().flatMap(this::fetchAllArchiveAndRespond);
  }

  public Mono<ResponseEntity<Flux<ByteBuffer>>> downloadArchiveById(@PathVariable("id") UUID id) {
    return archivesRepo.findById(id).flatMap(this::fetchAllArchiveAndRespond);
  }

  public Mono<Page<Archive>> getArchives(FindArchivesQuery findArchivesQuery) {
    return archivesRepo.findByCommand(findArchivesQuery);
  }

  public Mono<Archive> getArchive(UUID id) {
    return archivesRepo.findById(id);
  }

  private <T> Mono<EntityListResponse<T>> listResponseTransform(
      Mono<? extends Collection<T>> entities) {
    return entities.map(entityList -> EntityListResponse.<T>builder().data(entityList).build());
  }

  private Mono<ResponseEntity<Flux<ByteBuffer>>> fetchAllArchiveAndRespond(Archive archive) {
    return archivesRepo
        .save(incrementDownloads(archive))
        .flatMap(downloadObjectById::apply)
        .map(
            archiveDownload ->
                ResponseEntity.ok()
                    .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        format("attachment; filename=%s", parseFilenameFromArchive(archive)))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .body(archiveDownload.getFlux()))
        .onErrorContinue(
            (throwable, obj) ->
                // todo: rework this for correct error handling
                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header("X-Reason", "file-bundle-not-built")
                    .build());
  }

  private static String parseFilenameFromArchive(Archive archive) {
    if (archive.getType().equals(ArchiveType.ALL)) {
      // for a download all entry the hash info is the instant string
      return format("virusseq-consensus-export-all-%s.tar.gz", archive.getHashInfo());
    } else {
      // otherwise just note the archiveId with the download + todo: confirm this with someone!!!
      return format("virusseq-consensus-export-%s.tar.gz", archive.getId());
    }
  }

  private static Archive incrementDownloads(Archive archive) {
    archive.setNumOfDownloads(archive.getNumOfDownloads() + 1);
    return archive;
  }
}
