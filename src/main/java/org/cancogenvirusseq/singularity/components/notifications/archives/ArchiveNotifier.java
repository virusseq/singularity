package org.cancogenvirusseq.singularity.components.notifications.archives;

import lombok.AllArgsConstructor;
import org.cancogenvirusseq.singularity.components.notifications.IndexerNotification;
import org.cancogenvirusseq.singularity.components.notifications.NotificationName;
import org.cancogenvirusseq.singularity.components.notifications.Notifier;
import org.cancogenvirusseq.singularity.repository.model.Archive;
import org.cancogenvirusseq.singularity.repository.model.ArchiveStatus;
import org.cancogenvirusseq.singularity.repository.model.ArchiveType;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
public class ArchiveNotifier {

    private final Notifier notifier;

    public void notify(Archive archive) {
        if (archive != null) {
            Message message = new Message(archive.getStatus(), archive.getHash(), new Date(TimeUnit.SECONDS.toMillis(archive.getCreatedAt())));
            notifier.notify(new IndexerNotification(getNotificationName(archive.getType(), archive.getStatus()), message.toLinkedHashMap()));
        }
    }

    private NotificationName getNotificationName(ArchiveType type, ArchiveStatus status){
        NotificationName notificationName;
        switch (type){
            case ALL:
                notificationName = isErrorNotification(status) ? NotificationName.RELEASE_ERROR : NotificationName.RELEASE;
                break;
            case SET_QUERY:
                notificationName =isErrorNotification(status) ? NotificationName.SETS_ERROR : NotificationName.SETS;
                break;
            default:
                notificationName = NotificationName.ALL;
        }

        return notificationName;
    }

    private boolean isErrorNotification(ArchiveStatus status){
        return ArchiveStatus.FAILED.equals(status) ? true : false;
    }
}
