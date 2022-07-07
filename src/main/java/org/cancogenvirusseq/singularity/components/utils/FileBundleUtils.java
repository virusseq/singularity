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

package org.cancogenvirusseq.singularity.components.utils;

import static java.lang.String.format;
import static org.cancogenvirusseq.singularity.components.model.FileBundle.DOWNLOAD_DIR;
import static org.cancogenvirusseq.singularity.components.model.FileBundle.archiveFilenameFromArchiveId;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.cancogenvirusseq.singularity.components.model.AnalysisDocumentMolecularDataPair;
import org.cancogenvirusseq.singularity.components.model.FileBundle;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.springframework.util.FileSystemUtils;
import reactor.core.publisher.Flux;

@Slf4j
public class FileBundleUtils {

  public static Function<Flux<AnalysisDocumentMolecularDataPair>, Flux<Path>>
      createFileBundleFromPairsWithArchive(Archive archive) {
    return dataPairFlux ->
        dataPairFlux
            .reduce(new FileBundle(archive.getId()), addDownloadPairToFileBundle)
            .map(tarGzipArchiveAndClose)
            .flux()
            .log("Download::downloadAndArchiveFunctionWithInstant");
  }

  private static final BiConsumer<BufferedOutputStream, byte[]> writeToFileStream =
      (stream, bytes) -> {
        try {
          stream.write(bytes);
        } catch (IOException e) {
          log.error(e.getLocalizedMessage(), e);
        }
      };

  private static final BiFunction<FileBundle, AnalysisDocumentMolecularDataPair, FileBundle>
      addDownloadPairToFileBundle =
          (fileBundle, downloadPair) -> {
            writeToFileStream.accept(
                fileBundle.getMolecularFileOutputStream(), downloadPair.getMolecularData());
            return fileBundle;
          };

  private static final UnaryOperator<FileBundle> closeMolecularAndMetadataFileStreams =
      fileBundle -> {
        try {
          fileBundle.getMolecularFileOutputStream().close();
        } catch (IOException e) {
          log.error(e.getLocalizedMessage(), e);
        }
        return fileBundle;
      };

  private static final UnaryOperator<FileBundle> createGzipOutputStream =
      fileBundle -> {
        try {
          fileBundle.setArchiveGzipOutputStream(
              new GzipCompressorOutputStream(fileBundle.getArchiveFileOutputStream()));
        } catch (IOException e) {
          log.error(e.getLocalizedMessage(), e);
        }
        return fileBundle;
      };

  private static final UnaryOperator<FileBundle> createTarOutputStream =
      fileBundle -> {
        fileBundle.setArchiveTarOutputStream(
            new TarArchiveOutputStream(fileBundle.getArchiveGzipOutputStream()));
        return fileBundle;
      };

    private static final BiConsumer<GzipCompressorOutputStream, File> archiveFile =
       (gzipCompressorOutputStream, file) -> {
         try {
            InputStream in = new FileInputStream(file);
            final byte[] buffer = new byte[1024];
            int n = 0;
            while (-1 != (n = in.read(buffer))) {
              gzipCompressorOutputStream.write(buffer, 0, n);
            }
            gzipCompressorOutputStream.close();
            in.close();
         } catch (IOException e) {
             log.error(e.getLocalizedMessage(), e);
         }
       };

  private static final UnaryOperator<FileBundle> putBundleFilesInArchive =
      fileBundle -> {
        // put both bundle files in the archive
        archiveFile.accept(
            fileBundle.getArchiveGzipOutputStream(),
            new File(
                format(
                    "%s/%s",
                    fileBundle.getDownloadDirectory(), fileBundle.getMolecularFilename())));
        return fileBundle;
      };

  private static final UnaryOperator<FileBundle> closeAllStreams =
      fileBundle -> {
        // closing the ArchiveTarOutputStream cascades and closes the underlying gzip and buffered
        // file
        // input streams
        try {
          fileBundle.getArchiveGzipOutputStream().finish();
          fileBundle.getArchiveGzipOutputStream().close();
        } catch (IOException e) {
          log.error(e.getLocalizedMessage(), e);
        }
        return fileBundle;
      };

  private static final Function<FileBundle, Path> finalize =
      fileBundle -> {
        try {
          FileSystemUtils.deleteRecursively(Paths.get(fileBundle.getDownloadDirectory()));
        } catch (IOException e) {
          log.error(e.getLocalizedMessage(), e);
        }
        return Paths.get(format("%s/%s", DOWNLOAD_DIR, fileBundle.getArchiveFilename()));
      };

  public static final Consumer<Archive> deleteFileBundleForArchive =
      archive -> {
        try {
          FileSystemUtils.deleteRecursively(
              Paths.get(
                  format("%s/%s", DOWNLOAD_DIR, archiveFilenameFromArchiveId(archive.getId()))));
          log.debug(
              "File archive '{}/{}' deleted from disk",
              DOWNLOAD_DIR,
              archiveFilenameFromArchiveId(archive.getId()));
        } catch (IOException e) {
          log.error(e.getLocalizedMessage(), e);
        }
      };

  /**
   * Function that takes a fileBundle, closes it's files, generates the tar.gz, deletes the download
   * directory and returns the full path to the archive
   */
  public static final Function<FileBundle, Path> tarGzipArchiveAndClose =
      closeMolecularAndMetadataFileStreams
          .andThen(createGzipOutputStream)
          .andThen(putBundleFilesInArchive)
          .andThen(closeAllStreams)
          .andThen(finalize);
}
