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

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.OffsetDateTime;

@Data
@Document(indexName = "${elastic.fileCentricIndex}", createIndex = false)
public class AnalysisDocument {
  @Id private String objectId;
  private String studyId;
  private Analysis analysis;
  private Donor[] donors;

  @Data
  private static class Analysis {
    private Experiment experiment;
    private Host host;
    private LineageAnalysis lineageAnalysis;
    private SampleCollection sampleCollection;
    private SequenceAnalysis sequenceAnalysis;
  }

  @Data
  private static class Experiment {
    private String purposeOfSequencing;
    private String purposeOfSequencingDetails;
    private OffsetDateTime sequencingDate;
    private String libraryId;
    private String sequencingInstrument;
    private String sequencingProtocolName;
  }

  @Data
  private static class Host {
    private String hostAge;
    private String hostGender;
    private String hostAgeBin;
    private String hostDisease;
    private String hostAgeUnit;
    private String hostScientificName;
  }

  @Data
  private static class LineageAnalysis {
    private String lineageName;
    private String variantEvidence;
    private String variantDesignation;
    private String variantEvidenceDetails;
    private String lineageAnalysisSoftwareName;
    private String lineageAnalysisSoftwareVersion;
  }

  @Data
  private static class SampleCollection {
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
    private OffsetDateTime sampleCollectionDate;
    private String nmlSubmittedSpecimenType;
    private String purposeOfSamplingDetails;
    private String sampleCollectionDatePrecision;
  }

  @Data
  private static class SequenceAnalysis {
    private Metrics metrics;
    private String nsPer100kbp;
    private String depthOfCoverage;
    private String breadthOfCoverage;
    private String consensusGenomeLength;
  }

  @Data
  private static class Metrics {
    private String dehostingMethod;
    private String bioinformaticsProtocol;
    private String referenceGenomeAccession;
    private String consensusSequenceSoftwareName;
    private String consensusSequenceSoftwareVersion;
    private String rawSequenceDataProcessingMethod;
  }

  @Data
  private static class Donor {
    private String submitterDonorId;
  }
}
