package org.cancogenvirusseq.singularity.components.notifications.archives;

import lombok.AllArgsConstructor;
import org.cancogenvirusseq.singularity.components.notifications.IndexerNotification;
import org.cancogenvirusseq.singularity.components.notifications.NotificationName;
import org.cancogenvirusseq.singularity.components.notifications.Notifier;
import org.cancogenvirusseq.singularity.components.utils.DateConverter;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.repository.model.ArchiveType;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@AllArgsConstructor
public class ArchiveNotifier {

    private final Notifier notifier;

    private final DateConverter dateConverter;

    public void notify(Archive archive) {
        if (archive != null) {
            Message message = new Message(
                    archive.getStatus(),
                    archive.getHash(),
                    dateConverter.instantToZonedDateTime(Instant.ofEpochSecond(archive.getCreatedAt())));

            notifier.notify(
                    new IndexerNotification(
                            ArchiveType.ALL.equals(archive.getType()) ? NotificationName.RELEASE : NotificationName.SETS,
                            message.toLinkedHashMap()));
        }
    }
}
