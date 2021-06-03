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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AnalysisDocument {
  @NonNull private String objectId;
  @NonNull private String studyId;
  @NonNull private Analysis analysis;
  @NonNull private List<Donor> donors;

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class Analysis {
    @NonNull private Experiment experiment;
    @NonNull private Host host;
    @NonNull private LineageAnalysis lineageAnalysis;
    @NonNull private SampleCollection sampleCollection;
    @NonNull private SequenceAnalysis sequenceAnalysis;
  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class Experiment {
    private String purposeOfSequencing;
    private String purposeOfSequencingDetails;
    private String sequencingDate;
    private String libraryId;
    private String sequencingInstrument;
    private String sequencingProtocolName;
  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class Host {
    private String hostAge;
    private String hostGender;
    private String hostAgeBin;
    private String hostDisease;
    private String hostAgeUnit;
    private String hostScientificName;
  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class LineageAnalysis {
    private String lineageName;
    private String variantEvidence;
    private String variantDesignation;
    private String variantEvidenceDetails;
    private String lineageAnalysisSoftwareName;
    private String lineageAnalysisSoftwareVersion;
  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class SampleCollection {
    private String isolate;
    private String organism;
    private String bodyProduct;
    private String geoLocCity;
    private String anatomicalPart;
    private String geoLocCountry;
    private String geoLocProvince;
    private String collectionDevice;
    private String collectionMethod;
    private String environmentalSite;
    private String anatomicalMaterial;
    private String purposeOfSampling;
    private String sampleCollectedBy;
    private String sequenceSubmittedBy;
    private String environmentalMaterial;
    private String sampleCollectionDate;
    private String nmlSubmittedSpecimenType;
    private String purposeOfSamplingDetails;
    private String sampleCollectionDatePrecision;
  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class SequenceAnalysis {
    private String consensusSequenceSoftwareName;
    private String consensusSequenceSoftwareVersion;
    private String dehostingMethod;
    @NonNull private Metrics metrics;
    private String referenceGenomeAccession;
    private String rawSequenceDataProcessingMethod;
    private String bioinformaticsProtocol;
  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class Metrics {
    private String nsPer100kbp;
    private String depthOfCoverage;
    private String consensusGenomeLength;
    private String breadthOfCoverage;
  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class Donor {
    @NonNull private String submitterDonorId;
  }
}
