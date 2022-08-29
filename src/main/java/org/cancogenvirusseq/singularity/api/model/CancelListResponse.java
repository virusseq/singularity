package org.cancogenvirusseq.singularity.api.model;

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
  List<ErrorArchive> error;
  Summary summary;

}
