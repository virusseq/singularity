package org.cancogenvirusseq.singularity.config.db;

import static java.lang.String.format;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FlywayConfig {
  private final PostgresProperties postgresProperties;

  @Bean(initMethod = "migrate")
  public Flyway flyway() {
    return createFlyway(postgresProperties);
  }

  public static Flyway createFlyway(PostgresProperties postgresProperties) {
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
