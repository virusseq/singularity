package org.cancogenvirusseq.singularity.config.slack;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.cancogenvirusseq.singularity.components.notifications.NotificationName;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.Set;

@Data
@ToString
@EqualsAndHashCode
@Configuration
@ConfigurationProperties(prefix = "notifications.slack")
public class SlackProperties {
    private boolean enabled = false;
    private String url;
    private Templates templates = new Templates();
    private Set<String> notifiedOn = Set.of(NotificationName.ALL.name().toUpperCase());
    private int maxDataLength = 1000;
    private WebClient webClient = new WebClient();

    @Data
    @ToString
    @EqualsAndHashCode
    public static class Templates {
        private static String DEFAULT_TEMPLATE = "##TYPE## ##DATA##";
        private String error = DEFAULT_TEMPLATE;
        private String warning = DEFAULT_TEMPLATE;
        private String info = DEFAULT_TEMPLATE;
    }

    public int webClientMaxInMemorySize() {
        return this.webClient.getMaxInMemorySize();
    }
}
