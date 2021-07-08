package org.cancogenvirusseq.singularity.config.db;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
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
