package org.cancogenvirusseq.singularity.components.notifications.archives;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.cancogenvirusseq.singularity.components.notifications.IndexerNotification;
import org.cancogenvirusseq.singularity.components.notifications.NotificationName;
import org.cancogenvirusseq.singularity.components.notifications.Notifier;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.repository.model.ArchiveType;

@Component
@AllArgsConstructor
public class ArchiveNotifier {

    private final Notifier notifier;

    public void notify(Archive archive) {
        if (archive != null) {
            Message message = new Message(archive.getStatus(), archive.getHash(), new Date(TimeUnit.SECONDS.toMillis(archive.getCreatedAt())));
            notifier.notify(new IndexerNotification(ArchiveType.ALL.equals(archive.getType()) ? NotificationName.RELEASE : NotificationName.SETS, message.toLinkedHashMap()));
        }
    }
}
