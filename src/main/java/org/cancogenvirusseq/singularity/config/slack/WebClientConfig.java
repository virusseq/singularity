package org.cancogenvirusseq.singularity.config.slack;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class WebClientConfig {

    @Bean
    org.springframework.web.reactive.function.client.WebClient webClient(SlackProperties properties) {
        return org.springframework.web.reactive.function.client.WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(properties.webClientMaxInMemorySize()))
                .build();
    }
}
