package org.cancogenvirusseq.singularity.components.pipelines;

import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.components.events.ArchiveBuildRequestEmitter;
import org.cancogenvirusseq.singularity.components.hoc.ArchiveBuildRequestToArchive;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

@Slf4j
@Component
@RequiredArgsConstructor
public class SetQueryArchiveBuild {
  private final ArchiveBuildRequestEmitter archiveBuildRequestEmitter;
  private final ArchiveBuildRequestToArchive archiveBuildRequestToArchive;

  @Getter private Disposable setQueryArchiveBuildDisposable;

  @PostConstruct
  public void init() {
    setQueryArchiveBuildDisposable = createSetQueryArchiveBuildDisposable();
  }

  private Disposable createSetQueryArchiveBuildDisposable() {
    return archiveBuildRequestEmitter.receive().flatMap(archiveBuildRequestToArchive).subscribe();
  }
}
