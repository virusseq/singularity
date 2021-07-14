package org.cancogenvirusseq.singularity.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.cancogenvirusseq.singularity.repository.model.*;
import org.springframework.data.domain.*;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class ArchivesUnifiedRepo {
  private final ObjectMapper mapper = new ObjectMapper();
  private final ArchiveAllRepo archiveAllRepo;
  private final ArchiveSetQueryRepo archiveSetQueryRepo;
  private final DatabaseClient databaseClient;

  private final Function<Map<String, Object>, ArchiveAll> ARCHIVE_ALL_MAPPER =
      r -> {
        val metaMap = new HashMap<String, Object>();
        r.forEach(
            (k, v) -> {
              if (!k.startsWith("meta_")) {
                return;
              }
              val newKey = k.replace("meta_", "");
              metaMap.put(newKey, v);
            });
        val meta = mapper.convertValue(metaMap, ArchiveMeta.class);
        val archive = mapper.convertValue(r, ArchiveAll.class);
        archive.setMeta(meta);
        return archive;
      };

  private final ArchiveSort<ArchiveAll.Fields> DEFAULT_ARCHIVE_ALL_SORT =
      ArchiveSort.<ArchiveAll.Fields>builder()
          .fieldName(ArchiveAll.Fields.timestamp)
          .sortDirection(Sort.Direction.ASC)
          .build();

  public Mono<ArchiveAll> save(ArchiveAll var1) {
    return insertArchiveAll(var1);
  }

  public Mono<ArchiveAll> findById(UUID var1) {
    return selectArchiveAll(null, var1, 20, 0, DEFAULT_ARCHIVE_ALL_SORT).next();
  }

  public Flux<ArchiveSetQuery> findAll() {
    return archiveSetQueryRepo.findAll();
  }

  public Mono<Page<ArchiveAll>> findAllByStatus(
      ArchiveStatus status,
      @NonNull Integer size,
      @NonNull Integer offset,
      @NonNull ArchiveSort<ArchiveAll.Fields> sort) {

    val pageable =
        PageRequest.of(
            offset / size, size, Sort.by(sort.getSortDirection(), sort.getFieldName().toString()));

    val totalHitsMono = archiveAllRepo.countByStatus(status);
    return selectArchiveAll(status, null, size, offset, sort)
        .collectList()
        .zipWith(
            totalHitsMono, (archives, totalHits) -> new PageImpl<>(archives, pageable, totalHits));
  }

  private Flux<ArchiveAll> selectArchiveAll(
      ArchiveStatus status,
      UUID id,
      @NonNull Integer size,
      @NonNull Integer offset,
      @NonNull ArchiveSort<ArchiveAll.Fields> sort) {
    val s = new StringBuilder();
    s.append(" SELECT archive_all.*,");
    s.append(
        " archive_meta.num_of_samples as meta_num_of_samples, archive_meta.num_of_downloads as meta_num_of_downloads ");
    s.append(" FROM archive_all, archive_meta ");
    s.append(" where id = archive_id ");

    if (status != null) {
      s.append(" AND status=:status");
    }
    if (id != null) {
      s.append(" AND id=:id");
    }

    s.append("ORDER BY ").append(sort.getFieldName()).append(sort.getSortDirection());
    s.append("LIMIT ").append(size);
    s.append("OFFSET ").append(offset);

    val sql_statement = s.toString();

    DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql_statement);
    if (status != null) {
      spec = spec.bind("status", status);
    }
    if (id != null) {
      spec = spec.bind("id", id);
    }
    return spec.fetch().all().map(ARCHIVE_ALL_MAPPER);
  }

  private Mono<ArchiveAll> insertArchiveAll(ArchiveAll archiveAll) {
    String s =
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
}
