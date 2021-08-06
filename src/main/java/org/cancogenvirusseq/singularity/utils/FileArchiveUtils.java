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

package org.cancogenvirusseq.singularity.utils;

import static java.lang.String.format;
import static org.cancogenvirusseq.singularity.components.model.FilesArchive.DOWNLOAD_DIR;
import static org.cancogenvirusseq.singularity.components.model.FilesArchive.archiveFilenameFromInstant;
import static org.cancogenvirusseq.singularity.utils.CommonUtils.writeToFileStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.function.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.cancogenvirusseq.singularity.components.model.AnalysisDocumentMolecularDataPair;
import org.cancogenvirusseq.singularity.components.model.FilesArchive;
import org.springframework.util.FileSystemUtils;
import reactor.core.publisher.Flux;

@Slf4j
public class FileArchiveUtils {

  public static Function<Flux<AnalysisDocumentMolecularDataPair>, Flux<Path>>
      createArchiveFromPairsWithInstant(Instant instant) {
    return dataPairFlux ->
        dataPairFlux
            .reduce(new FilesArchive(instant), addDownloadPairToFileArchive)
            .map(tarArchiveAndClose)
            .flux()
            .log("Download::downloadAndArchiveFunctionWithInstant");
  }

  private static final BiFunction<FilesArchive, AnalysisDocumentMolecularDataPair, FilesArchive>
      addDownloadPairToFileArchive =
          (filesArchive, downloadPair) -> {
            writeToFileStream.accept(
                filesArchive.getMolecularFileOutputStream(), downloadPair.getMolecularData());
            writeToFileStream.accept(
                filesArchive.getMetadataFileOutputStream(),
                TsvUtils.analysisDocumentToTsvRowBytes(downloadPair.getAnalysisDocument()));
            return filesArchive;
          };

  private static final UnaryOperator<FilesArchive> closeMolecularAndMetadataFileStreams =
      filesArchive -> {
        try {
          filesArchive.getMolecularFileOutputStream().close();
          filesArchive.getMetadataFileOutputStream().close();
        } catch (IOException e) {
          log.error(e.getLocalizedMessage(), e);
        }
        return filesArchive;
      };

  private static final UnaryOperator<FilesArchive> createTarOutputStream =
      filesArchive -> {
        filesArchive.setArchiveTarOutputStream(
            new TarArchiveOutputStream(filesArchive.getArchiveFileOutputStream()));
        return filesArchive;
      };

  private static final BiConsumer<TarArchiveOutputStream, File> archiveFile =
      (tarArchiveOutputStream, file) -> {
        try {
          tarArchiveOutputStream.putArchiveEntry(new TarArchiveEntry(file, file.getName()));
          IOUtils.copy(new BufferedInputStream(new FileInputStream(file)), tarArchiveOutputStream);
          tarArchiveOutputStream.closeArchiveEntry();
        } catch (IOException e) {
          log.error(e.getLocalizedMessage(), e);
        }
      };

  private static final UnaryOperator<FilesArchive> putBundleFilesInArchive =
      filesArchive -> {

        // put both bundle files in the archive
        archiveFile.accept(
            filesArchive.getArchiveTarOutputStream(),
            new File(
                format(
                    "%s/%s",
                    filesArchive.getDownloadDirectory(), filesArchive.getMolecularFilename())));
        archiveFile.accept(
            filesArchive.getArchiveTarOutputStream(),
            new File(
                format(
                    "%s/%s",
                    filesArchive.getDownloadDirectory(), filesArchive.getMetadataFilename())));

        // return the FileBundle
        return filesArchive;
      };

  private static final UnaryOperator<FilesArchive> closeAllStreams =
      filesArchive -> {
        try {
          filesArchive.getArchiveTarOutputStream().close();
        } catch (IOException e) {
          log.error(e.getLocalizedMessage(), e);
        }
        return filesArchive;
      };

  private static final Function<FilesArchive, Path> finalize =
      filesArchive -> {
        try {
          FileSystemUtils.deleteRecursively(Paths.get(filesArchive.getDownloadDirectory()));
        } catch (IOException e) {
          log.error(e.getLocalizedMessage(), e);
        }
        return Paths.get(format("%s/%s", DOWNLOAD_DIR, filesArchive.getArchiveFilename()));
      };

  public static final Consumer<Instant> deleteArchiveForInstant =
      instant -> {
        try {
          FileSystemUtils.deleteRecursively(
              Paths.get(format("%s/%s", DOWNLOAD_DIR, archiveFilenameFromInstant(instant))));
          log.debug(
              "File archive '{}/{}' deleted from disk",
              DOWNLOAD_DIR,
              archiveFilenameFromInstant(instant));
        } catch (IOException e) {
          log.error(e.getLocalizedMessage(), e);
        }
      };

  /**
   * Function that takes a fileBundle, closes it's files, generates the tar.gz, deletes the download
   * directory and returns the full path to the archive
   */
  public static final Function<FilesArchive, Path> tarArchiveAndClose =
      closeMolecularAndMetadataFileStreams
          .andThen(createTarOutputStream)
          .andThen(putBundleFilesInArchive)
          .andThen(closeAllStreams)
          .andThen(finalize);
}
