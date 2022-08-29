package org.cancogenvirusseq.singularity.api.model;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "A list of items")
public class ItemsRequest<String> {
  Collection<String> items;
}
