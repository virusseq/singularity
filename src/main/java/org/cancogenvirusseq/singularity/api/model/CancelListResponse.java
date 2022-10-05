package org.cancogenvirusseq.singularity.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "A list response containing entities of the requested type")
public class CancelListResponse {
  List<HashResult> data;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  List<ErrorArchive> error;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  List<String> ignored;
  Summary summary;

}
