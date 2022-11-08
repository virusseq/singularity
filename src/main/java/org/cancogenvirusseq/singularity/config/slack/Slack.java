package org.cancogenvirusseq.singularity.config.slack;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cancogenvirusseq.singularity.components.notifications.IndexerNotification;
import org.cancogenvirusseq.singularity.components.notifications.NotificationChannel;
import org.cancogenvirusseq.singularity.components.notifications.NotificationName;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class Slack implements NotificationChannel {

    private static final String TYPE = "##TYPE##";
    private static final String DATA = "##DATA##";
    private final WebClient webClient;
    private final SlackProperties slackChannelInfo;

    public Slack(SlackProperties properties, WebClient webClient) {
        this.webClient = webClient;
        this.slackChannelInfo = properties;
    }

    @Override
    public Mono<Boolean> send(@NonNull IndexerNotification notification) {
        var template = this.slackChannelInfo.getTemplates().getInfo();
        switch (notification.getNotificationName().getCategory()) {
            case ERROR:
                template = this.slackChannelInfo.getTemplates().getError();
                break;
            case WARN:
                template = this.slackChannelInfo.getTemplates().getWarning();
                break;
        }
        val mapAsString = notification
                .getAttributes()
                .keySet()
                .stream()
                .map(key -> "> *" + key + "*:" + notification.getAttributes().get(key))
                .collect(Collectors.joining("\n"));
        val dataStringTruncated =
                "\n" + mapAsString.substring(
                        0, Math.min(mapAsString.length(), this.slackChannelInfo.getMaxDataLength() - 1));

        val text =
                template
                        .replace(TYPE, notification.getNotificationName().getHeader())
                        .replace(DATA, dataStringTruncated);

        val payload =
                Map.of(
                        "text", text);

        return this.webClient
                .post()
                .uri(this.slackChannelInfo.getUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(payload))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(3L))
                .map((ignored) -> true)
                .onErrorResume(
                        e -> {
                            log.error("failed to send message to slack", e);
                            return Mono.just(false);
                        });
    }

    @Override
    public Set<NotificationName> subscriptions() {
        return slackChannelInfo.isEnabled() ?
                Set.copyOf(slackChannelInfo
                        .getNotifiedOn()
                        .stream()
                        .map(NotificationName::valueOf)
                        .collect(Collectors.toUnmodifiableSet())) :
                Set.of();
    }
}
