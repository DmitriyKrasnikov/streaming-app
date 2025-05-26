package com.krasnikov.streaming.config;

import com.krasnikov.streaming.model.BlockedUserEvent;
import com.krasnikov.streaming.model.ChatMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // --------------- ОБЩИЕ ФАБРИКИ ----------------

    private <T> ProducerFactory<String, T> jsonProducerFactory() {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
        );
        return new DefaultKafkaProducerFactory<>(props);
    }

    private <T> KafkaTemplate<String, T> jsonKafkaTemplate(ProducerFactory<String, T> pf) {
        return new KafkaTemplate<>(pf);
    }

    private <T> ConsumerFactory<String, T> jsonConsumerFactory(
            Class<T> targetClass,
            String groupId
    ) {
        JsonDeserializer<T> deserializer = new JsonDeserializer<>(targetClass);
        deserializer.addTrustedPackages("com.krasnikov.streaming.model");
        deserializer.setUseTypeHeaders(false);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T>
    kafkaListenerFactory(ConsumerFactory<String, T> cf) {

        ConcurrentKafkaListenerContainerFactory<String, T> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cf);
        return factory;
    }

    // --------------- БИНЫ ДЛЯ ChatMessage ----------------

    @Bean
    public ProducerFactory<String, ChatMessage> chatMessageProducerFactory() {
        return jsonProducerFactory();
    }

    @Bean
    public KafkaTemplate<String, ChatMessage> chatMessageKafkaTemplate() {
        return jsonKafkaTemplate(chatMessageProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, ChatMessage> chatMessageConsumerFactory() {
        return jsonConsumerFactory(ChatMessage.class, "chat-message-group");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ChatMessage>
    chatMessageListenerFactory() {
        return kafkaListenerFactory(chatMessageConsumerFactory());
    }

    // --------------- БИНЫ ДЛЯ BlockedUserEvent ----------------

    @Bean
    public ProducerFactory<String, BlockedUserEvent> blockedUserEventProducerFactory() {
        return jsonProducerFactory();
    }

    @Bean
    public KafkaTemplate<String, BlockedUserEvent> blockedUserEventKafkaTemplate() {
        return jsonKafkaTemplate(blockedUserEventProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, BlockedUserEvent> blockedUserEventConsumerFactory() {
        return jsonConsumerFactory(BlockedUserEvent.class, "blocked-user-event-group");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BlockedUserEvent>
    blockedUserEventListenerFactory() {
        return kafkaListenerFactory(blockedUserEventConsumerFactory());
    }

    // --------------- БИНЫ ДЛЯ String ----------------

    @Bean
    public ProducerFactory<String, String> stringProducerFactory() {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
        );
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> stringKafkaTemplate() {
        return new KafkaTemplate<>(stringProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, String> stringConsumerFactory() {
        Map<String, Object> props = new HashMap<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "string-group",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        ));
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
    stringListenerFactory() {
        return kafkaListenerFactory(stringConsumerFactory());
    }
}