package org.cancogenvirusseq.singularity.repository;

import java.util.UUID;
import org.cancogenvirusseq.singularity.repository.model.ArchiveSetQuery;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface ArchiveSetQueryRepo extends ReactiveCrudRepository<ArchiveSetQuery, UUID> {}
