package org.cancogenvirusseq.singularity.config.db;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.lang.String.format;

@Configuration
@RequiredArgsConstructor
public class FlywayConfig {
  private final PostgresProperties postgresProperties;

  @Bean(initMethod = "migrate")
  public Flyway flyway() {
    val url =
        format(
            "jdbc:postgresql://%s:%s/%s",
            postgresProperties.getHost(),
            postgresProperties.getPort(),
            postgresProperties.getDatabase());
    return new Flyway(
        Flyway.configure()
            .dataSource(url, postgresProperties.getUsername(), postgresProperties.getPassword()));
  }
}
