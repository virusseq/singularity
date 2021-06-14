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
import lombok.Getter;
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

  @Getter
  private static final String[] esIncludeFields =
      new String[] {
        "object_id",
        "study_id",
        // experiment
        "analysis.experiment.purpose_of_sequencing",
        "analysis.experiment.purpose_of_sequencing_details",
        "analysis.experiment.sequencing_date",
        "analysis.experiment.library_id",
        "analysis.experiment.sequencing_instrument",
        "analysis.experiment.sequencing_protocol_name",
        // host
        "analysis.host.host_age",
        "analysis.host.host_gender",
        "analysis.host.host_age_bin",
        "analysis.host.host_disease",
        "analysis.host.host_age_unit",
        "analysis.host.host_scientific_name",
        // lineage_analysis
        "analysis.lineage_analysis.lineage_name",
        "analysis.lineage_analysis.variant_evidence",
        "analysis.lineage_analysis.variant_designation",
        "analysis.lineage_analysis.variant_evidence_details",
        "analysis.lineage_analysis.lineage_analysis_software_name",
        "analysis.lineage_analysis.lineage_analysis_software_version",
        // sample_collection
        "analysis.sample_collection.isolate",
        "analysis.sample_collection.organism",
        "analysis.sample_collection.body_product",
        "analysis.sample_collection.geo_loc_city",
        "analysis.sample_collection.anatomical_part",
        "analysis.sample_collection.geo_loc_country",
        "analysis.sample_collection.geo_loc_province",
        "analysis.sample_collection.collection_device",
        "analysis.sample_collection.collection_method",
        "analysis.sample_collection.environmental_site",
        "analysis.sample_collection.anatomical_material",
        "analysis.sample_collection.purpose_of_sampling",
        "analysis.sample_collection.sample_collected_by",
        "analysis.sample_collection.sequence_submitted_by",
        "analysis.sample_collection.environmental_material",
        "analysis.sample_collection.sample_collection_date",
        "analysis.sample_collection.nml_submitted_specimen_type",
        "analysis.sample_collection.purpose_of_sampling_details",
        "analysis.sample_collection.sample_collection_date_precision",
        // sequence_analysis
        "analysis.sequence_analysis.consensus_sequence_software_name",
        "analysis.sequence_analysis.consensus_sequence_software_version",
        "analysis.sequence_analysis.dehosting_method",
        "analysis.sequence_analysis.metrics.Ns_per_100kbp",
        "analysis.sequence_analysis.metrics.breadth_of_coverage",
        "analysis.sequence_analysis.metrics.consensus_genome_length",
        "analysis.sequence_analysis.metrics.depth_of_coverage",
        "analysis.sequence_analysis.reference_genome_accession",
        "analysis.sequence_analysis.raw_sequence_data_processing_method",
        "analysis.sequence_analysis.bioinformatics_protocol",
        // donors
        "donors.submitter_donor_id"
      };
}