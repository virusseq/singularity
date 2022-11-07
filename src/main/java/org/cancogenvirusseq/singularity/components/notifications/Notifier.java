package org.cancogenvirusseq.singularity.components.notifications;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class Notifier {

    private final Set<NotificationChannel> notificationChannels;

    public Notifier(Set<NotificationChannel> notificationChannels) {
        this.notificationChannels = notificationChannels;
    }

    /**
     * Asynchronously calls the eligible notification channels.
     *
     * @param notification the notification to send
     */
    public void notify(IndexerNotification notification) {
        // some of the channels may need async I/O execution (slack) but
        // the caller (indexer) doesn't need to worry about what happens here so no need to return the
        // flux and we subscribe here.
        Flux.fromIterable(getEligibleChannels(notification))
                .flatMap(
                        notificationChannel ->
                                sendNotificationThroughChannel(notification, notificationChannel))
                .subscribe();
    }

    private Mono<Boolean> sendNotificationThroughChannel(
            IndexerNotification notification, NotificationChannel notificationChannel) {
        return notificationChannel
                .send(notification)
                .doOnSuccess((resp) -> log.info("Notification sent {} to channel {}", notification.getNotificationName(), notificationChannel.getClass().getName()))
                .onErrorResume(
                        e -> {
                            log.error(
                                    "failed to deliver notification {} to channel {}",
                                    notification,
                                    notificationChannel,
                                    e);
                            return Mono.just(false);
                        });
    }

    private List<NotificationChannel> getEligibleChannels(IndexerNotification notification) {
        return notificationChannels.stream()
                .filter(notificationChannel -> shouldReceiveNotification(notification, notificationChannel))
                .collect(Collectors.toUnmodifiableList());
    }

    private boolean shouldReceiveNotification(
            IndexerNotification notification, NotificationChannel notificationChannel) {
        return notificationChannel.subscriptions().contains(NotificationName.ALL)
                || notificationChannel.subscriptions().contains(notification.getNotificationName());
    }
}
