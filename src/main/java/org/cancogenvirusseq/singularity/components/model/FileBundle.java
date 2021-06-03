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

package org.cancogenvirusseq.singularity.components.model;

import static java.lang.String.format;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.cancogenvirusseq.singularity.components.TsvWriter;

@Getter
public class FileBundle {
  private static final String DOWNLOAD_DIR = "/tmp";
  private static final String FILE_NAME_TEMPLATE = "virusseq-consensus-export-all-";
  public static final String MOLECULAR_FILE_EXTENSION = ".fasta";
  public static final String METADATA_FILE_EXTENSION = ".tsv";

  private final String directory;
  private final FileOutputStream molecularFile;
  private final FileOutputStream metadataFile;

  @SneakyThrows
  public FileBundle(Instant instant) {
    this.directory = format("%s/%s%s", DOWNLOAD_DIR, FILE_NAME_TEMPLATE, instant);
    this.molecularFile =
        new FileOutputStream(
            format(
                "%s/%s%s%s",
                this.directory, FILE_NAME_TEMPLATE, instant, MOLECULAR_FILE_EXTENSION));
    this.metadataFile =
        new FileOutputStream(
            format(
                "%s/%s%s%s", this.directory, FILE_NAME_TEMPLATE, instant, METADATA_FILE_EXTENSION));
    this.metadataFile.write(TsvWriter.getHeader());
  }

  @SneakyThrows
  public String tarGzipAndCleanup() {
    molecularFile.close();
    metadataFile.close();

    OutputStream fOut = Files.newOutputStream(Paths.get("output.tar.gz"));
    BufferedOutputStream buffOut = new BufferedOutputStream(fOut);
    GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(buffOut);
    TarArchiveOutputStream tOut = new TarArchiveOutputStream(gzOut);

    TarArchiveEntry tarEntry = new TarArchiveEntry(file, fileName);

    tOut.putArchiveEntry(tarEntry);

    // copy file to TarArchiveOutputStream
    Files.copy(path, tOut);

    tOut.closeArchiveEntry();

    tOut.finish();

    return "";
  }
}
