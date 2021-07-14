package org.cancogenvirusseq.singularity.repository;

import static java.lang.String.format;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.cancogenvirusseq.singularity.repository.model.ArchiveSort.DEFAULT_ARCHIVE_SET_QUERY_SORT;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.cancogenvirusseq.singularity.repository.commands.SelectArchiveAllCommand;
import org.cancogenvirusseq.singularity.repository.model.*;
import org.springframework.data.domain.*;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

/**
 * This repository implements save and find functions backed by customized insert and select sql
 * statements to properly handle the relations between ArchiveAll/ArchiveSetQuery and ArchiveMeta.
 * The reason for doing it this way is because Spring-R2DBC-data doesn't support complex ORM
 * functionalities yet.
 */
@Repository
@RequiredArgsConstructor
public class ArchivesUnifiedCustomRepo {
  private final ObjectMapper mapper = new ObjectMapper();
  private final ArchiveAllRepo archiveAllRepo;
  private final ArchiveSetQueryRepo archiveSetQueryRepo;
  private final DatabaseClient databaseClient;

  private final Function<Map<String, Object>, ArchiveAll> ARCHIVE_ALL_MAPPER =
      r -> {
        val metaMap =
            r.entrySet().stream()
                .filter(e -> e.getKey().startsWith("meta_"))
                .collect(
                    Collectors.toMap(e -> e.getKey().replace("meta_", ""), Map.Entry::getValue));

        val meta = mapper.convertValue(metaMap, ArchiveMeta.class);
        val archive = mapper.convertValue(r, ArchiveAll.class);
        archive.setMeta(meta);
        return archive;
      };

  private final Function<Map<String, Object>, ArchiveSetQuery> ARCHIVE_SET_QUERY_MAPPER =
      r -> {
        val metaMap =
            r.entrySet().stream()
                .filter(e -> e.getKey().startsWith("meta_"))
                .collect(
                    Collectors.toMap(e -> e.getKey().replace("meta_", ""), Map.Entry::getValue));

        val meta = mapper.convertValue(metaMap, ArchiveMeta.class);
        val archive = mapper.convertValue(r, ArchiveSetQuery.class);

        archive.setMeta(meta);
        return archive;
      };

  public Mono<ArchiveAll> save(ArchiveAll archive) {
    return insertArchiveAll(archive);
  }

  public Mono<ArchiveSetQuery> save(ArchiveSetQuery archive) {
    return insertArchiveSetQuery(archive);
  }

  public Mono<ArchiveAll> findArchiveAllById(@NonNull UUID var1) {
    val command = SelectArchiveAllCommand.builder().id(var1).build();
    return selectArchiveAll(command).map(page -> page.getContent().get(0));
  }

  public Mono<ArchiveSetQuery> findArchiveSetQueryById(UUID id) {
    return selectArchiveSetQuery(null, id, 1, 0, DEFAULT_ARCHIVE_SET_QUERY_SORT).next();
  }

  public Flux<ArchiveSetQuery> findAll() {
    return archiveSetQueryRepo.findAll();
  }

  public Mono<Page<ArchiveAll>> findArchiveAllByCommand(SelectArchiveAllCommand command) {
    return selectArchiveAll(command);
  }

  private Mono<Page<ArchiveAll>> selectArchiveAll(SelectArchiveAllCommand command) {
    val status = command.getStatus();
    val id = command.getId();

    val sql_statement =
        " SELECT archive_all.*, archive_meta.num_of_samples as meta_num_of_samples, archive_meta.num_of_downloads as meta_num_of_downloads, count(*) OVER() AS count "
            + " FROM archive_all, archive_meta "
            + " where id = archive_id "
            + (status.isPresent() ? " AND status=:status " : "")
            + (id.isPresent() ? " AND id=:id " : "")
            + format(" ORDER BY %s %s ", command.getSortField(), command.getSortDirection())
            + " LIMIT :size "
            + " OFFSET :offset ";

    DatabaseClient.GenericExecuteSpec spec =
        databaseClient
            .sql(sql_statement)
            .bind("size", command.getSize())
            .bind("offset", command.getOffset());

    spec = status.isPresent() ? spec.bind("status", status.get()) : spec;
    spec = id.isPresent() ? spec.bind("id", id.get()) : spec;

    return spec.fetch()
        .all()
        .log()
        .collectList()
        .map(rl -> Tuples.of((Long) rl.get(0).getOrDefault("count", command.getSize()), rl))
        .map(
            t2 -> {
              val totalCount = t2.getT1();
              val archives =
                  t2.getT2().stream().map(ARCHIVE_ALL_MAPPER).collect(toUnmodifiableList());
              return new PageImpl<>(archives, command.cretePageable(), totalCount);
            });
  }

  private Flux<ArchiveSetQuery> selectArchiveSetQuery(
      ArchiveStatus status,
      UUID id,
      @NonNull Integer size,
      @NonNull Integer offset,
      @NonNull ArchiveSort<ArchiveSetQuery.Fields> sort) {
    val sql_statement =
        " SELECT archive_set_query.*, archive_meta.num_of_samples as meta_num_of_samples, archive_meta.num_of_downloads as meta_num_of_downloads "
            + " FROM archive_set_query, archive_meta "
            + " where id = archive_id "
            + (status != null ? " AND status=:status " : "")
            + (id != null ? " AND id=:id " : "")
            + " ORDER BY "
            + sort.getFieldName()
            + " "
            + sort.getSortDirection()
            + " LIMIT "
            + size
            + " "
            + " OFFSET "
            + offset
            + " ";

    DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql_statement);

    spec = status != null ? spec.bind("status", status) : spec;
    spec = id != null ? spec.bind("id", id) : spec;

    return spec.fetch().all().map(ARCHIVE_SET_QUERY_MAPPER);
  }

  private Mono<ArchiveAll> insertArchiveAll(ArchiveAll archiveAll) {
    val s =
        "WITH new_archive AS ( "
            + "INSERT INTO archive_all(status, timestamp, object_id) "
            + " VALUES (:status, :timestamp, :object_id) "
            + " RETURNING id )"
            + "INSERT INTO archive_meta(archive_id, num_of_downloads, num_of_samples)"
            + "VALUES ((SELECT id FROM new_archive), :num_of_downloads, :num_of_samples)"
            + "RETURNING archive_id as id";
    return databaseClient
        .sql(s)
        .bind("status", archiveAll.getStatus())
        .bind("timestamp", archiveAll.getTimestamp())
        .bind("object_id", archiveAll.getObjectId())
        .bind("num_of_downloads", archiveAll.getMeta().getNumOfDownloads())
        .bind("num_of_samples", archiveAll.getMeta().getNumOfSamples())
        .fetch()
        .first()
        .map(
            r -> {
              val id = UUID.fromString(r.get("id").toString());
              return archiveAll.toBuilder().id(id).build();
            });
  }

  private Mono<ArchiveSetQuery> insertArchiveSetQuery(ArchiveSetQuery archiveSetQuery) {
    val s =
        "WITH new_archive AS ( "
            + "INSERT INTO archive_set_query(status, timestamp, object_id, set_query_hash) "
            + " VALUES (:status, :timestamp, :object_id, :set_query_hash) "
            + " RETURNING id )"
            + "INSERT INTO archive_meta(archive_id, num_of_downloads, num_of_samples)"
            + "VALUES ((SELECT id FROM new_archive), :num_of_downloads, :num_of_samples)"
            + "RETURNING archive_id as id";
    return databaseClient
        .sql(s)
        .bind(ArchiveSetQuery.Fields.status.toString(), archiveSetQuery.getStatus())
        .bind(ArchiveSetQuery.Fields.timestamp.toString(), archiveSetQuery.getTimestamp())
        .bind(ArchiveSetQuery.Fields.objectId.toString(), archiveSetQuery.getObjectId())
        .bind(ArchiveSetQuery.Fields.setQueryHash.toString(), archiveSetQuery.getSetQueryHash())
        .bind(
            ArchiveMeta.Fields.numOfDownloads.toString(),
            archiveSetQuery.getMeta().getNumOfDownloads())
        .bind(
            ArchiveMeta.Fields.numOfSamples.toString(), archiveSetQuery.getMeta().getNumOfSamples())
        .fetch()
        .first()
        .map(
            r -> {
              val id = UUID.fromString(r.get("id").toString());
              return archiveSetQuery.toBuilder().id(id).build();
            });
  }
}
