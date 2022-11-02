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

package org.cancogenvirusseq.singularity.components.hoc;

import static org.cancogenvirusseq.singularity.components.utils.FileBundleUtils.createFileBundleFromPairsWithArchive;
import static org.cancogenvirusseq.singularity.components.utils.FileBundleUtils.deleteFileBundleForArchive;

import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.components.base.DownloadMolecularDataToPair;
import org.cancogenvirusseq.singularity.components.base.ElasticSearchScroll;
import org.cancogenvirusseq.singularity.components.base.FileBundleUpload;
import org.cancogenvirusseq.singularity.components.model.ArchiveBuildRequest;
import org.cancogenvirusseq.singularity.components.notifications.archives.ArchiveNotifier;
import org.cancogenvirusseq.singularity.repository.ArchivesRepo;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.repository.model.ArchiveStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveBuildRequestToArchive implements Function<ArchiveBuildRequest, Flux<Archive>> {

  private final ElasticSearchScroll elasticSearchScroll;
  private final DownloadMolecularDataToPair downloadMolecularDataToPair;
  private final FileBundleUpload fileBundleUpload;
  private final ArchivesRepo archivesRepo;

  private final ArchiveNotifier notifier;

  @Override
  public Flux<Archive> apply(ArchiveBuildRequest archiveBuildRequest) {
    return elasticSearchScroll
        .apply(archiveBuildRequest.getQueryBuilder())
        .transform(downloadMolecularDataToPair)
        .transform(createFileBundleFromPairsWithArchive(archiveBuildRequest.getArchive()))
        .flatMap(fileBundleUpload)
        .flatMap(
            uploadObjectId ->
                withArchiveBuildRequestContext(
                    archiveBuildRequestCtx -> {
                        archiveBuildRequestCtx.getArchive().setObjectId(uploadObjectId);
                        archiveBuildRequestCtx.getArchive().setStatus(ArchiveStatus.COMPLETE);

                        log.debug("processArchiveBuildRequest is done!");

                        notifier.notify(archiveBuildRequestCtx.getArchive());
                        return archivesRepo.save(archiveBuildRequestCtx.getArchive());
                    }))
        .onErrorResume(
            throwable ->
                withArchiveBuildRequestContext(
                    archiveBuildRequestCtx -> {
                      archiveBuildRequestCtx.getArchive().setStatus(ArchiveStatus.FAILED);
                      log.error(
                          "processArchiveBuildRequest error: {}", throwable.getLocalizedMessage());
                      notifier.notify(archiveBuildRequestCtx.getArchive());
                      return archivesRepo.save(archiveBuildRequestCtx.getArchive());
                    }))
        .doFinally(
          signalType -> deleteFileBundleForArchive.accept(archiveBuildRequest.getArchive()))
        .doOnCancel(() -> {
          archiveBuildRequest.getArchive().setStatus(ArchiveStatus.CANCELLED);
          log.info(
            "doOnCancel archive id:{} hash'{}' tagged as {}",
            archiveBuildRequest.getArchive().getId(),
            archiveBuildRequest.getArchive().getHash(),
            archiveBuildRequest.getArchive().getStatus()
            );
          notifier.notify(archiveBuildRequest.getArchive());
          archivesRepo.save(archiveBuildRequest.getArchive()).subscribe();
        })
        .contextWrite(ctx -> ctx.put("archiveBuildRequest", archiveBuildRequest))
        .log("ArchiveBuildRequestToArchive");
  }

  private <R> Mono<R> withArchiveBuildRequestContext(Function<ArchiveBuildRequest, Mono<R>> func) {
    return Mono.deferContextual(ctx -> func.apply(ctx.get("archiveBuildRequest")));
  }
}
