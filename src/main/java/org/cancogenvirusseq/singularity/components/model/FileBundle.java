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

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.cancogenvirusseq.singularity.components.TsvWriter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static java.lang.String.format;

@Slf4j
@Getter
public class FileBundle {
  public static final String DOWNLOAD_DIR = "/tmp";
  public static final String FILE_NAME_TEMPLATE = "virusseq-consensus-export-all-";
  public static final String MOLECULAR_FILE_EXTENSION = ".fasta";
  public static final String METADATA_FILE_EXTENSION = ".tsv";

  private final String directory;
  private final String molecularFilename;
  private final String metadataFilename;
  private final FileOutputStream molecularFile;
  private final FileOutputStream metadataFile;

  @SneakyThrows
  public FileBundle(Instant instant) {
    // create bundle directory
    this.directory = format("%s/%s%s", DOWNLOAD_DIR, FILE_NAME_TEMPLATE, instant);
    Files.createDirectory(Paths.get(this.directory));

    // record molecular filename and create FileOutputStream
    this.molecularFilename =
        format("%s%s%s", FILE_NAME_TEMPLATE, instant, MOLECULAR_FILE_EXTENSION);
    this.molecularFile =
        new FileOutputStream(getFileLocation(this.directory, this.molecularFilename));

    // record molecular filename and create FileOutputStream
    this.metadataFilename = format("%s%s%s", FILE_NAME_TEMPLATE, instant, METADATA_FILE_EXTENSION);
    this.metadataFile =
        new FileOutputStream(getFileLocation(this.directory, this.metadataFilename));

    // write the tsv header
    this.metadataFile.write(TsvWriter.getHeader());
  }

  private static final UnaryOperator<FileBundle> closeFiles =
      fileBundle -> {
        try {
          fileBundle.getMolecularFile().close();
          fileBundle.getMetadataFile().close();
        } catch (IOException e) {
          log.error(e.getLocalizedMessage(), e);
        }
        return fileBundle;
      };

  private static final UnaryOperator<FileBundle> tarGzipDirectory =
      fileBundle ->
          Optional.of(getFileOutputStream(fileBundle.getDirectory()))
              .map(BufferedOutputStream::new)
              .map(FileBundle::getGzipCompressorOutputStream)
              .map(TarArchiveOutputStream::new)
              .map(
                  tarArchiveOutputStream ->
                      putBundleFilesInArchive(tarArchiveOutputStream, fileBundle))
              .orElseThrow();

  private static final Function<FileBundle, String> finalize =
      fileBundle -> {
        // todo: delete directory
        return getArchivePath(fileBundle.getDirectory());
      };

  public static final Function<FileBundle, String> tarGzipBundleAndClose =
      closeFiles.andThen(tarGzipDirectory).andThen(finalize);

  @SneakyThrows
  private static FileOutputStream getFileOutputStream(String directory) {
    return new FileOutputStream(getArchivePath(directory));
  }

  private static String getArchivePath(String directory) {
    return format("%s.tar.gz", directory);
  }

  @SneakyThrows
  private static GzipCompressorOutputStream getGzipCompressorOutputStream(
      BufferedOutputStream bufferedOutputStream) {
    return new GzipCompressorOutputStream(bufferedOutputStream);
  }

  @SneakyThrows
  private static FileBundle putBundleFilesInArchive(
      TarArchiveOutputStream tarArchiveOutputStream, FileBundle fileBundle) {
    // add entries
    tarArchiveOutputStream.putArchiveEntry(
        new TarArchiveEntry(
            new File(getFileLocation(fileBundle.getDirectory(), fileBundle.getMolecularFilename())),
            fileBundle.getMolecularFilename()));
    tarArchiveOutputStream.putArchiveEntry(
        new TarArchiveEntry(
            new File(getFileLocation(fileBundle.getDirectory(), fileBundle.getMetadataFilename())),
            fileBundle.getMetadataFilename()));
    
    // copy files
    IOUtils.copy(
        new BufferedInputStream(
            new FileInputStream(
                getFileLocation(fileBundle.getDirectory(), fileBundle.getMolecularFilename()))),
        tarArchiveOutputStream);
    IOUtils.copy(
        new BufferedInputStream(
            new FileInputStream(
                getFileLocation(fileBundle.getDirectory(), fileBundle.getMetadataFilename()))),
        tarArchiveOutputStream);

    // close and finish
    tarArchiveOutputStream.closeArchiveEntry();
    tarArchiveOutputStream.finish();

    // return the FileBundle
    return fileBundle;
  }

  private static String getFileLocation(String directory, String filename) {
    return format("%s/%s", directory, filename);
  }
}
