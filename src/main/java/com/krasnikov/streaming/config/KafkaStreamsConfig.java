package com.krasnikov.streaming.config;

import com.krasnikov.streaming.model.BlockedUserEvent;
import com.krasnikov.streaming.model.ChatMessage;
import com.krasnikov.streaming.streams.BlockListService;
import com.krasnikov.streaming.streams.CensorshipService;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaStreamsConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${application.topic.messages}")
    private String inputTopic;

    @Value("${application.topic.filtered-messages}")
    private String outputTopic;

    @Value("${application.topic.blocked-users}")
    private String blockedUsersTopic;

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kafkaStreamsConfiguration() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streaming-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    public JsonSerde<BlockedUserEvent> blockedUserEventSerde() {
        JsonSerde<BlockedUserEvent> serde = new JsonSerde<>(BlockedUserEvent.class);
        serde.deserializer().setUseTypeHeaders(false);
        serde.deserializer().addTrustedPackages("com.krasnikov.streaming.model");
        return serde;
    }

    @Bean
    public JsonSerde<ChatMessage> chatMessageSerde() {
        JsonSerde<ChatMessage> serde = new JsonSerde<>(ChatMessage.class);
        serde.deserializer().setUseTypeHeaders(false);
        serde.deserializer().addTrustedPackages("com.krasnikov.streaming.model");
        return serde;
    }

    @Bean
    public GlobalKTable<String, BlockedUserEvent> blockedUsersTable(
            StreamsBuilder builder,
            JsonSerde<BlockedUserEvent> blockedUserEventSerde
    ) {
        return builder.globalTable(
                blockedUsersTopic,
                Consumed.with(Serdes.String(), blockedUserEventSerde),
                Materialized.<String, BlockedUserEvent, KeyValueStore<Bytes, byte[]>>as("blocked-users-store")
                        .withKeySerde(Serdes.String())
                        .withValueSerde(blockedUserEventSerde)
        );
    }

    @Bean
    public KStream<String, ChatMessage> messageStream(
            StreamsBuilder builder,
            JsonSerde<ChatMessage> chatMessageSerde,
            BlockListService blockListService,
            CensorshipService censorshipService
    ) {
        KStream<String, ChatMessage> stream = builder.stream(
                inputTopic,
                Consumed.with(Serdes.String(), chatMessageSerde)
        );

        stream.filter((key, msg) -> !blockListService.isBlocked(msg.senderId(), msg.recipientId()))
                .mapValues(censorshipService::censor)
                .to(outputTopic, Produced.with(Serdes.String(), chatMessageSerde));

        return stream;
    }
}