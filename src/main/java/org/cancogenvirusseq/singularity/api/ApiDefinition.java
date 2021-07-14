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
import java.util.UUID;
import org.cancogenvirusseq.singularity.api.model.EntityListResponse;
import org.cancogenvirusseq.singularity.api.model.ErrorResponse;
import org.cancogenvirusseq.singularity.repository.commands.SelectArchiveAllCommand;
import org.cancogenvirusseq.singularity.repository.commands.SelectArchiveSetQueryCommand;
import org.cancogenvirusseq.singularity.repository.model.ArchiveAll;
import org.cancogenvirusseq.singularity.repository.model.ArchiveSetQuery;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

@CrossOrigin
@Api(value = "Singularity - All Contributors, All Files", tags = "Singularity")
public interface ApiDefinition {
  String UNKNOWN_MSG = "An unexpected error occurred.";

  @ApiOperation(
      value = "Get All Contributors",
      nickname = "Get Contributors",
      response = EntityListResponse.class,
      tags = "Singularity")
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
      value = "Download all molecular files as a single .fasta.gz gzip compressed file",
      nickname = "Download Files",
      response = MultipartFile.class,
      tags = "Singularity")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "", response = MultipartFile.class),
        @ApiResponse(code = 500, message = UNKNOWN_MSG, response = ErrorResponse.class)
      })
  @RequestMapping(
      value = "/files",
      produces = MediaType.APPLICATION_OCTET_STREAM_VALUE,
      method = RequestMethod.GET)
  ResponseEntity<Mono<Resource>> getFiles();

  @ApiOperation(
      value = "Get a archives of a specific status and their details.",
      nickname = "ArchiveAll Details",
      tags = "Singularity")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "", response = Object.class),
        @ApiResponse(code = 500, message = UNKNOWN_MSG, response = ErrorResponse.class)
      })
  @RequestMapping(
      value = "/archives/all",
      produces = MediaType.APPLICATION_JSON_VALUE,
      method = RequestMethod.GET)
  Mono<Page<ArchiveAll>> getArchiveAll(SelectArchiveAllCommand fetchArchivesRequest);

  @ApiOperation(
      value = "Get a archives of a specific status and their details.",
      nickname = "ArchiveAll Details",
      tags = "Singularity")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "", response = ArchiveAll.class),
        @ApiResponse(code = 500, message = UNKNOWN_MSG, response = ErrorResponse.class)
      })
  @RequestMapping(
      value = "/archives/all/{id}",
      produces = MediaType.APPLICATION_JSON_VALUE,
      method = RequestMethod.GET)
  Mono<ArchiveAll> getArchiveAllById(@RequestParam UUID id);

  @ApiOperation(
      value = "Get a archives of a specific status and their details.",
      nickname = "ArchiveAll Details",
      tags = "Singularity")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "", response = Object.class),
        @ApiResponse(code = 500, message = UNKNOWN_MSG, response = ErrorResponse.class)
      })
  @RequestMapping(
      value = "/archives/set-query",
      produces = MediaType.APPLICATION_JSON_VALUE,
      method = RequestMethod.GET)
  Mono<Page<ArchiveSetQuery>> getArchiveSetQuery(SelectArchiveSetQueryCommand command);

  @ApiOperation(
      value = "Get a archives of a specific status and their details.",
      nickname = "ArchiveAll Details",
      tags = "Singularity")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "", response = Object.class),
        @ApiResponse(code = 500, message = UNKNOWN_MSG, response = ErrorResponse.class)
      })
  @RequestMapping(
      value = "/archives/set-query/{id}",
      produces = MediaType.APPLICATION_JSON_VALUE,
      method = RequestMethod.GET)
  Mono<ArchiveSetQuery> getArchiveSetQueryById(@RequestParam UUID id);
}
