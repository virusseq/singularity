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

package org.cancogenvirusseq.all.api;

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.all.api.model.EntityListResponse;
import org.cancogenvirusseq.all.service.ContributorService;
import org.cancogenvirusseq.all.service.DownloadService;
import org.cancogenvirusseq.all.service.model.AnalysisSnippet;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ApiController implements ApiDefinition {
  private final ContributorService contributorService;
  private final DownloadService downloadService;

  public Mono<EntityListResponse<String>> getContributors() {
    return contributorService.getContributors().transform(this::listResponseTransform);
  }

  public Mono<EntityListResponse<AnalysisSnippet>> getFiles() {
    return downloadService.getAllAnalyses().collectList().transform(this::listResponseTransform);
  }

  //  public ResponseEntity<Mono<DataBuffer>> getFiles() {
  //    return null;
  //  }

  private <T> Mono<EntityListResponse<T>> listResponseTransform(
      Mono<? extends Collection<T>> entities) {
    return entities.map(entityList -> EntityListResponse.<T>builder().data(entityList).build());
  }
}
