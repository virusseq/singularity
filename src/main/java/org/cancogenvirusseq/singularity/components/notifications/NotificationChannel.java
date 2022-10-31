package org.cancogenvirusseq.singularity.components.notifications;

import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * A channel is an abstraction of the technology infrastructure that this notification will be
 * delivered through, can be email, web api call, filesystem or anything.
 */
public interface NotificationChannel {
    Mono<Boolean> send(@NonNull IndexerNotification notification);

    Set<NotificationName> subscriptions();
}
