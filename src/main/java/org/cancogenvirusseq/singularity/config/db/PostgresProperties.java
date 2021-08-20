package org.cancogenvirusseq.singularity.config.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "postgres")
public class PostgresProperties {
  private String host;
  private int port;
  private String database;
  private String username;
  private String password;
  private Integer maxPoolIdleTimeMs;
  private Integer maxPoolSize;
}
