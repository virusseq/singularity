package org.cancogenvirusseq.singularity.components.events;

import org.cancogenvirusseq.singularity.components.pipelines.AllArchiveBuild;
import org.cancogenvirusseq.singularity.config.kafka.KafkaProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ArchiveBuildKafkaListener {

    private KafkaProperties kafkaProperties;
    private final String topic;

    @Autowired
    private AllArchiveBuild allArchiveBuild;

    public ArchiveBuildKafkaListener(KafkaProperties properties) {
        kafkaProperties = properties;
        topic = properties.getTopic();
    }

    @KafkaListener(id = "listen1", topics = "release_archive")
    public void listen1(String in) {
        System.out.println(in);
        allArchiveBuild.createBuildAllArchiveDisposable(Instant.now());
    }
}
