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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Slf4j
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AnalysisDocument {
  public static final String ID_FIELD = "_id";
  public static final String LAST_UPDATED_AT_FIELD = "analysis.updatedAt";

  @NonNull
  private JsonNode objectId;
  @NonNull
  private JsonNode studyId;
  @NonNull
  private Analysis analysis;

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class Analysis {
    private Experiment experiment = new Experiment();
    private DatabaseIdentifiers databaseIdentifiers = new DatabaseIdentifiers();
    private Host host = new Host();
    private LineageAnalysis lineageAnalysis = new LineageAnalysis();
    private PathogenDiagnosticTesting pathogenDiagnosticTesting = new PathogenDiagnosticTesting();
    private SampleCollection sampleCollection = new SampleCollection();
    private SequenceAnalysis sequenceAnalysis = new SequenceAnalysis();
    private JsonNode firstPublishedAt;
    @NonNull
    private List<Sample> samples;

    @JsonProperty("updatedAt")
    private JsonNode lastUpdatedAt;

    public void setFirstPublishedAt(JsonNode firstPublishedAt) {
      try {
        // firstPublishedAt is stored in Epoch millisecond which should be a long
        long epoch = Long.parseLong(firstPublishedAt.toString());
        Date date = Date.from(Instant.ofEpochMilli(epoch));
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        this.firstPublishedAt = JsonNodeFactory.instance.textNode(dateFormat.format(date));
      } catch (Exception e) {
        log.error("Couldn't convert analysis.firstPublishedAt", e);
        this.firstPublishedAt = JsonNodeFactory.instance.textNode("");
      }
    }

    public void setLastUpdatedAt(JsonNode updatedAt) {
      try {
        // updatedAt is stored in Epoch millisecond which should be a long
        long epoch = Long.parseLong(updatedAt.toString());
        Date date = Date.from(Instant.ofEpochMilli(epoch));
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        this.lastUpdatedAt = JsonNodeFactory.instance.textNode(dateFormat.format(date));
      } catch (Exception e) {
        log.error("Couldn't convert analysis.lastUpdatedAt", e);
        this.lastUpdatedAt = JsonNodeFactory.instance.textNode("");
      }
    }
  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class LineageAnalysis {
    private JsonNode lineageName;
    private JsonNode lineageAnalysisSoftwareName;
    private JsonNode lineageAnalysisSoftwareVersion;
    private JsonNode lineageAnalysisSoftwareDataVersion;
    private JsonNode scorpioCall;
    private JsonNode scorpioVersion;
  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class Experiment {
    private JsonNode purposeOfSequencing;
    private JsonNode purposeOfSequencingDetails;
    private JsonNode sequencingInstrument;
    private JsonNode sequencingProtocol;

  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class DatabaseIdentifiers {
    private JsonNode gisaidAccession;
  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class Host {
    private JsonNode hostAge;
    private JsonNode hostAgeNullReason;
    private JsonNode hostGender;
    private JsonNode hostAgeBin;
    private JsonNode hostDisease;
    private JsonNode hostAgeUnit;
    private JsonNode hostScientificName;
  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class PathogenDiagnosticTesting {
    private JsonNode geneName;
    private JsonNode diagnosticPcrCtValue;
    private JsonNode diagnosticPcrCtValueNullReason;
  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class SampleCollection {
    private JsonNode isolate;
    private JsonNode fastaHeaderName;
    private JsonNode organism;
    private JsonNode bodyProduct;
    private JsonNode anatomicalPart;
    private JsonNode geoLocCountry;
    private JsonNode geoLocProvince;
    private JsonNode collectionDevice;
    private JsonNode collectionMethod;
    private JsonNode environmentalSite;
    private JsonNode anatomicalMaterial;
    private JsonNode purposeOfSampling;
    private JsonNode sampleCollectedBy;
    private JsonNode sequenceSubmittedBy;
    private JsonNode environmentalMaterial;
    private JsonNode sampleCollectionDate;
    private JsonNode purposeOfSamplingDetails;
    private JsonNode sampleCollectionDateNullReason;
  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class SequenceAnalysis {
    private JsonNode consensusSequenceSoftwareName;
    private JsonNode consensusSequenceSoftwareVersion;
    private JsonNode dehostingMethod;
    @NonNull
    private Metrics metrics;
    private JsonNode referenceGenomeAccession;
    private JsonNode rawSequenceDataProcessingMethod;
    private JsonNode bioinformaticsProtocol;
  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class Metrics {
    private JsonNode depthOfCoverage;
    private JsonNode breadthOfCoverage;
  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class Donor {
    @NonNull
    private JsonNode submitterDonorId;
  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class Sample {
    @NonNull
    private Donor donor;
  }


  @Getter
  private static final String[] esIncludeFields =
    new String[]{
      "object_id",
      "study_id",
      // analysis
      "analysis.firstPublishedAt",
      "analysis.updatedAt",
      // experiment
      "analysis.experiment.purpose_of_sequencing",
      "analysis.experiment.purpose_of_sequencing_details",
      "analysis.experiment.sequencing_instrument",
      "analysis.experiment.sequencing_protocol",
      // database_identifiers
      "analysis.database_identifiers.gisaid_accession",
      // host
      "analysis.host.host_age",
      "analysis.host.host_age_null_reason",
      "analysis.host.host_gender",
      "analysis.host.host_age_bin",
      "analysis.host.host_disease",
      "analysis.host.host_age_unit",
      "analysis.host.host_scientific_name",
      //lineage
      "analysis.lineage_analysis.lineage_name",
      "analysis.lineage_analysis.lineage_analysis_software_name",
      "analysis.lineage_analysis.lineage_analysis_software_version",
      "analysis.lineage_analysis.lineage_analysis_software_data_version",
      "analysis.lineage_analysis.scorpio_call",
      "analysis.lineage_analysis.scorpio_version",
      // pathogen diagnostic testing
      "analysis.pathogen_diagnostic_testing.gene_name",
      "analysis.pathogen_diagnostic_testing.diagnostic_pcr_ct_value",
      "analysis.pathogen_diagnostic_testing.diagnostic_pcr_ct_value_null_reason",
      // sample_collection
      "analysis.sample_collection.isolate",
      "analysis.sample_collection.fasta_header_name",
      "analysis.sample_collection.organism",
      "analysis.sample_collection.body_product",
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
      "analysis.sample_collection.purpose_of_sampling_details",
      "analysis.sample_collection.sample_collection_date_null_reason",
      // sequence_analysis
      "analysis.sequence_analysis.consensus_sequence_software_name",
      "analysis.sequence_analysis.consensus_sequence_software_version",
      "analysis.sequence_analysis.dehosting_method",
      "analysis.sequence_analysis.metrics.breadth_of_coverage",
      "analysis.sequence_analysis.metrics.depth_of_coverage",
      "analysis.sequence_analysis.reference_genome_accession",
      "analysis.sequence_analysis.raw_sequence_data_processing_method",
      "analysis.sequence_analysis.bioinformatics_protocol",
      // donors
      "analysis.samples.donor.submitterDonorId",
    };
}
