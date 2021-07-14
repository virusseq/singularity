package org.cancogenvirusseq.singularity.repository;

import static org.cancogenvirusseq.singularity.config.db.FlywayConfig.createFlyway;
import static org.cancogenvirusseq.singularity.config.db.R2DBCConfiguration.createPsqlConnectionFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import lombok.val;
import org.cancogenvirusseq.singularity.config.db.PostgresProperties;
import org.cancogenvirusseq.singularity.repository.model.ArchiveAll;
import org.cancogenvirusseq.singularity.repository.model.ArchiveMeta;
import org.cancogenvirusseq.singularity.repository.model.ArchiveStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.r2dbc.core.DatabaseClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

@Testcontainers
public class ArchiveUnifiedRepoTests {
  @Container
  public PostgreSQLContainer postgreSQLContainer =
      new PostgreSQLContainer("postgres:10-alpine")
          .withDatabaseName("singularity")
          .withUsername("test")
          .withPassword("test");

  private ArchivesUnifiedRepo repo;

  @BeforeEach
  public void setUp() {
    val mockArchiveAllRepo = Mockito.mock(ArchiveAllRepo.class);
    val nockArchiveSetQueryRepo = Mockito.mock(ArchiveSetQueryRepo.class);

    val postgresProps = getPostgresProperties();

    // run migrations on test container db
    val flyway = createFlyway(postgresProps);
    flyway.migrate();

    // setup db client for repo
    val databaseClient =
        DatabaseClient.builder()
            .connectionFactory(createPsqlConnectionFactory(postgresProps))
            .build();

    repo = new ArchivesUnifiedRepo(mockArchiveAllRepo, nockArchiveSetQueryRepo, databaseClient);
  }

  static final ArchiveAll ARCHIVE_ALL_STUB_0 =
      ArchiveAll.builder()
          .status(ArchiveStatus.COMPLETE)
          .objectId(UUID.randomUUID())
          .timestamp(2L)
          .meta(ArchiveMeta.builder().numOfDownloads(4).numOfSamples(100).build())
          .build();

  @Test
  public void testSimpleInsert() {
    StepVerifier.create(repo.save(ARCHIVE_ALL_STUB_0).log())
        .assertNext(
            a -> {
              val stubWithGeneratedId = ARCHIVE_ALL_STUB_0.toBuilder().id(a.getId()).build();
              assertEquals(stubWithGeneratedId, a);
            })
        .verifyComplete();
  }

  private PostgresProperties getPostgresProperties() {
    return PostgresProperties.builder()
        .port(postgreSQLContainer.getFirstMappedPort())
        .host(postgreSQLContainer.getHost())
        .database(postgreSQLContainer.getDatabaseName())
        .password(postgreSQLContainer.getPassword())
        .username(postgreSQLContainer.getUsername())
        .build();
  }
}
