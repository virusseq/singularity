package org.cancogenvirusseq.singularity.repository;

import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Interface describing functions that need custom implementation to have proper insert and querying.
 *
 * Java doesn't have a way to pick interface methods, so this interface acts as a
 * sudo partial interface of {@link org.springframework.data.repository.reactive.ReactiveCrudRepository}.
 *
 * Some functions have <S extends Archive> because the method needs to have the
 * same signatures as the one in ReactiveCrudRepository for them to be overridden
 */
public interface ArchiveCustomRepo {
    Flux<Archive> findAll();

    <S extends Archive> Mono<S> save(S var1);

    <S extends Archive> Flux<S> saveAll(Iterable<S> var1);

    <S extends Archive> Flux<S> saveAll(Publisher<S> var1);

    Mono<Archive> findById(UUID var1);

    Mono<Archive> findById(Publisher<UUID> var1);
}
