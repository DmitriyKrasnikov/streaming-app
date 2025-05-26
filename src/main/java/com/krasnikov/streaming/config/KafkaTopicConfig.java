package com.krasnikov.streaming.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${application.topic.messages}")
    private String messagesTopic;

    @Value("${application.topic.filtered-messages}")
    private String filteredMessagesTopic;

    @Value("${application.topic.blocked-users}")
    private String blockedUsersTopic;

    @Value("${application.topic.forbidden-words}")
    private String forbiddenWordsTopic;

    @Bean
    public NewTopic messagesTopic() {
        return TopicBuilder.name(messagesTopic)
                .partitions(3)
                .replicas(3)
                .config("min.insync.replicas", "2")
                .build();
    }

    @Bean
    public NewTopic filteredMessagesTopic() {
        return TopicBuilder.name(filteredMessagesTopic)
                .partitions(3)
                .replicas(3)
                .config("min.insync.replicas", "2")
                .build();
    }

    @Bean
    public NewTopic blockedUsersTopic() {
        return TopicBuilder.name(blockedUsersTopic)
                .partitions(1)
                .replicas(3)
                .compact()
                .config("min.insync.replicas", "2")
                .build();
    }

    @Bean
    public NewTopic forbiddenWordsTopic() {
        return TopicBuilder.name(forbiddenWordsTopic)
                .partitions(1)
                .replicas(3)
                .compact()
                .config("min.insync.replicas", "2")
                .build();
    }
}