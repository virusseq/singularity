package org.cancogenvirusseq.singularity.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.repository.model.ArchiveMeta;
import org.reactivestreams.Publisher;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toMap;

@Repository
@RequiredArgsConstructor
public class ArchiveCustomRepoImpl implements ArchiveCustomRepo {
    private final DatabaseClient databaseClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String FIND_ALL_QUERY = "SELECT archive.*, archive_meta.num_of_downloads as meta_num_of_downloads FROM public.archive, public.archive_meta where id = archive_id;";

    @Override
    public Flux<Archive> findAll() {
         return databaseClient
                        .sql(FIND_ALL_QUERY)
                        .fetch()
                        .all()
                        .map(r -> {
                            val metaMap = new HashMap<String, Object>();
                            r.forEach((k, v) -> {
                                  val newKey = k.replace("meta_", "");
                                  metaMap.put(newKey, v);
                              });
                            val meta = mapper.convertValue(metaMap, ArchiveMeta.class);
                            val archive = mapper.convertValue(r, Archive.class);
                            archive.setMeta(meta);
                            return archive;
                        });
    }

    @Override
    public Mono<Archive> save(Archive var1) {
        return null;
    }

    @Override
    public <S extends Archive> Flux<S> saveAll(Iterable<S> var1) {
        return null;
    }

    @Override
    public <S extends Archive> Flux<S> saveAll(Publisher<S> var1) {
        return null;
    }

    @Override
    public Mono<Archive> findById(UUID var1) {
        return null;
    }

    @Override
    public Mono<Archive> findById(Publisher<UUID> var1) {
        return null;
    }
}
