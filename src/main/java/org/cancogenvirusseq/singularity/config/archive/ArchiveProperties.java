package org.cancogenvirusseq.singularity.config.archive;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "archive")

public class ArchiveProperties {
  Long cancelPeriodSeconds;
  Long maxBuildingSeconds;
}