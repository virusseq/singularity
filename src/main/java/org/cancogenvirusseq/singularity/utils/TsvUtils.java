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

import lombok.Getter;
import org.cancogenvirusseq.singularity.components.model.AnalysisDocument;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TsvUtils {
  @Getter
  private static final byte[] header =
      (String.join(
                  "\t",
                  List.of(
                      "study_id",
                      "specimen collector sample ID",
                      "sample collected by",
                      "sequence submitted by",
                      "sample collection date",
                      "sample collection date null reason",
                      "geo_loc_name (country)",
                      "geo_loc_name (state/province/territory)",
                      "organism",
                      "isolate",
                      "purpose of sampling",
                      "purpose of sampling details",
                      "anatomical material",
                      "anatomical part",
                      "body product",
                      "environmental material",
                      "environmental site",
                      "collection device",
                      "collection method",
                      "host (scientific name)",
                      "host disease",
                      "host age",
                      "host age null reason",
                      "host age unit",
                      "host age bin",
                      "host gender",
                      "purpose of sequencing",
                      "purpose of sequencing details",
                      "sequencing instrument",
                      "sequencing protocol",
                      "raw sequence data processing method",
                      "dehosting method",
                      "consensus sequence software name",
                      "consensus sequence software version",
                      "breadth of coverage value",
                      "depth of coverage value",
                      "reference genome accession",
                      "bioinformatics protocol",
                      "gene name 1",
                      "diagnostic pcr Ct value 1"))
              + "\n")
          .getBytes(StandardCharsets.UTF_8);

  public static byte[] analysisDocumentsToTsvRowsBytes(List<AnalysisDocument> analysisDocuments) {
    return (analysisDocuments.stream()
                .map(TsvUtils::analysisDocumentToTsvRow)
                .collect(Collectors.joining("\n"))
            + // join rows with newline
            "\n" // append newline to final batch of rows
        )
        .getBytes(StandardCharsets.UTF_8);
  }

  private static String analysisDocumentToTsvRow(AnalysisDocument analysisDocument) {
    return stringsToTsvRow(
        analysisDocument.getStudyId(),
        analysisDocument.getDonors().get(0).getSubmitterDonorId(),
        analysisDocument.getAnalysis().getSampleCollection().getSampleCollectedBy(),
        analysisDocument.getAnalysis().getSampleCollection().getSequenceSubmittedBy(),
        analysisDocument.getAnalysis().getSampleCollection().getSampleCollectionDate(),
        analysisDocument.getAnalysis().getSampleCollection().getSampleCollectionDateNullReason(),
        analysisDocument.getAnalysis().getSampleCollection().getGeoLocCountry(),
        analysisDocument.getAnalysis().getSampleCollection().getGeoLocProvince(),
        analysisDocument.getAnalysis().getSampleCollection().getOrganism(),
        analysisDocument.getAnalysis().getSampleCollection().getIsolate(),
        analysisDocument.getAnalysis().getSampleCollection().getPurposeOfSampling(),
        analysisDocument.getAnalysis().getSampleCollection().getPurposeOfSamplingDetails(),
        analysisDocument.getAnalysis().getSampleCollection().getAnatomicalMaterial(),
        analysisDocument.getAnalysis().getSampleCollection().getAnatomicalPart(),
        analysisDocument.getAnalysis().getSampleCollection().getBodyProduct(),
        analysisDocument.getAnalysis().getSampleCollection().getEnvironmentalMaterial(),
        analysisDocument.getAnalysis().getSampleCollection().getEnvironmentalSite(),
        analysisDocument.getAnalysis().getSampleCollection().getCollectionDevice(),
        analysisDocument.getAnalysis().getSampleCollection().getCollectionMethod(),
        analysisDocument.getAnalysis().getHost().getHostScientificName(),
        analysisDocument.getAnalysis().getHost().getHostDisease(),
        analysisDocument.getAnalysis().getHost().getHostAge(),
        analysisDocument.getAnalysis().getHost().getHostAgeNullReason(),
        analysisDocument.getAnalysis().getHost().getHostAgeUnit(),
        analysisDocument.getAnalysis().getHost().getHostAgeBin(),
        analysisDocument.getAnalysis().getHost().getHostGender(),
        analysisDocument.getAnalysis().getExperiment().getPurposeOfSequencing(),
        analysisDocument.getAnalysis().getExperiment().getPurposeOfSequencingDetails(),
        analysisDocument.getAnalysis().getExperiment().getSequencingInstrument(),
        analysisDocument.getAnalysis().getExperiment().getSequencingProtocol(),
        analysisDocument.getAnalysis().getSequenceAnalysis().getRawSequenceDataProcessingMethod(),
        analysisDocument.getAnalysis().getSequenceAnalysis().getDehostingMethod(),
        analysisDocument.getAnalysis().getSequenceAnalysis().getConsensusSequenceSoftwareName(),
        analysisDocument.getAnalysis().getSequenceAnalysis().getConsensusSequenceSoftwareVersion(),
        analysisDocument.getAnalysis().getSequenceAnalysis().getMetrics().getBreadthOfCoverage(),
        analysisDocument.getAnalysis().getSequenceAnalysis().getMetrics().getDepthOfCoverage(),
        analysisDocument.getAnalysis().getSequenceAnalysis().getReferenceGenomeAccession(),
        analysisDocument.getAnalysis().getSequenceAnalysis().getBioinformaticsProtocol(),
        analysisDocument.getAnalysis().getPathogenDiagnosticTesting().getGeneName1(),
        analysisDocument.getAnalysis().getPathogenDiagnosticTesting().getDiagnosticPcrProtocol1());
  }

  private static String stringsToTsvRow(String... strings) {
    return Optional.of(
            Arrays.stream(strings)
                .reduce(
                    new StringBuilder(),
                    (acc, curr) -> {
                      acc.append(valueIfPresentOrEmpty(curr));
                      acc.append("\t");
                      return acc;
                    },
                    StringBuilder::append)
                .toString())
        .map(row -> row.substring(0, row.length() - 1)) // trim trailing "\t"
        .orElse("");
  }

  private static String valueIfPresentOrEmpty(@Nullable String s) {
    return Optional.ofNullable(s).orElse("");
  }
}
