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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.nio.ByteBuffer;
import java.util.UUID;
import org.cancogenvirusseq.singularity.api.model.EntityListResponse;
import org.cancogenvirusseq.singularity.api.model.ErrorResponse;
import org.cancogenvirusseq.singularity.api.model.SetIdBuildRequest;
import org.cancogenvirusseq.singularity.components.model.TotalCounts;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.repository.query.FindArchivesQuery;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@CrossOrigin
@Api(value = "Singularity - All Contributors, All Files", tags = "Singularity API")
public interface ApiDefinition {
  String UNKNOWN_MSG = "An unexpected error occurred.";

  @ApiOperation(
      value = "Get All Contributors",
      nickname = "Get Contributors",
      response = EntityListResponse.class,
      tags = "Singularity API")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "", response = EntityListResponse.class),
        @ApiResponse(code = 500, message = UNKNOWN_MSG, response = ErrorResponse.class)
      })
  @RequestMapping(
      value = "/contributors",
      produces = MediaType.APPLICATION_JSON_VALUE,
      method = RequestMethod.GET)
  Mono<EntityListResponse<String>> getContributors();

  @ApiOperation(
          value = "Get total counts of samples, files and studies",
          nickname = "Get Total Counts",
          response = EntityListResponse.class,
          tags = "Singularity API")
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "", response = TotalCounts.class),
                  @ApiResponse(code = 500, message = UNKNOWN_MSG, response = ErrorResponse.class)
          })
  @RequestMapping(
          value = "/aggregations/total-counts",
          produces = MediaType.APPLICATION_JSON_VALUE,
          method = RequestMethod.GET)
  Mono<ResponseEntity<TotalCounts>> getTotalCounts();

  @ApiOperation(
      value = "Download the latest data archive containing all molecular and meta data",
      nickname = "Download All",
      response = MultipartFile.class,
      tags = "Singularity API")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "", response = MultipartFile.class),
        @ApiResponse(code = 500, message = UNKNOWN_MSG, response = ErrorResponse.class)
      })
  @RequestMapping(
      value = "/download/archive/all",
      produces = MediaType.APPLICATION_OCTET_STREAM_VALUE,
      method = RequestMethod.GET)
  @Transactional
  Mono<ResponseEntity<Flux<ByteBuffer>>> downloadLatestAllArchive();

  @ApiOperation(
      value = "Download an archive by ID",
      nickname = "Download Archive by ID",
      response = MultipartFile.class,
      tags = "Singularity API")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "", response = MultipartFile.class),
        @ApiResponse(code = 500, message = UNKNOWN_MSG, response = ErrorResponse.class)
      })
  @RequestMapping(
      value = "/download/archive/{id}",
      produces = MediaType.APPLICATION_OCTET_STREAM_VALUE,
      method = RequestMethod.GET)
  @Transactional
  Mono<ResponseEntity<Flux<ByteBuffer>>> downloadArchiveById(@PathVariable("id") UUID id);

  @ApiOperation(
      value = "Get details of any archives that bundles all sample data.",
      nickname = "Archive",
      tags = "Singularity API")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "", response = Object.class),
        @ApiResponse(code = 500, message = UNKNOWN_MSG, response = ErrorResponse.class)
      })
  @RequestMapping(
      value = "/archives",
      produces = MediaType.APPLICATION_JSON_VALUE,
      method = RequestMethod.GET)
  Mono<Page<Archive>> getArchives(FindArchivesQuery req);

  @ApiOperation(
      value = "Get details of a specific archive that bundle sample data.",
      nickname = "Archive",
      tags = "Singularity API")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "", response = Archive.class),
        @ApiResponse(code = 500, message = UNKNOWN_MSG, response = ErrorResponse.class)
      })
  @RequestMapping(
      value = "/archives/{id}",
      produces = MediaType.APPLICATION_JSON_VALUE,
      method = RequestMethod.GET)
  Mono<Archive> getArchive(@PathVariable("id") UUID id);

  @ApiOperation(
      value = "Build a new set query archive given set id",
      nickname = "Build Set Query Archive",
      tags = "Singularity API")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "", response = Archive.class),
        @ApiResponse(code = 500, message = UNKNOWN_MSG, response = ErrorResponse.class)
      })
  @RequestMapping(
      value = "/build-archive/set-query",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE,
      method = RequestMethod.POST)
  Mono<Archive> buildArchiveWithSetId(@RequestBody SetIdBuildRequest setIdBuildRequest);
}
