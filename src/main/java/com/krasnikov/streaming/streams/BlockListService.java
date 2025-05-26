package com.krasnikov.streaming.streams;

import com.krasnikov.streaming.model.BlockedUserEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class BlockListService {

    private final StreamsBuilderFactoryBean factoryBean;
    private final KafkaTemplate<String, BlockedUserEvent> blockedUserEventKafkaTemplate;
    private ReadOnlyKeyValueStore<String, BlockedUserEvent> store;

    @Value("${application.topic.blocked-users}")
    private String blockedUsersTopic;

    @Autowired
    public BlockListService(
            @Qualifier("blockedUserEventKafkaTemplate") KafkaTemplate<String, BlockedUserEvent> blockedUserEventKafkaTemplate,
            StreamsBuilderFactoryBean factoryBean) {
        this.factoryBean = factoryBean;
        this.blockedUserEventKafkaTemplate = blockedUserEventKafkaTemplate;
    }

    @PostConstruct
    public void registerStateListener() {
        factoryBean.setStateListener((newState, oldState) -> {
            if (newState == KafkaStreams.State.RUNNING) {
                initStore();
            }
        });
        if (factoryBean.isRunning()) {
            initStore();
        }
    }

    private void initStore() {
        this.store = factoryBean.getKafkaStreams().store(StoreQueryParameters.
                fromNameAndType("blocked-users-store", QueryableStoreTypes.keyValueStore())
        );
        log.info("BlockListService state store initialized");
    }

    public boolean isBlocked(String senderId, String recipientId) {
        String compositeKey = recipientId + ":" + senderId;
        return store != null && store.get(compositeKey) != null;
    }

    public void publishBlockEvent(String userId, String blockedUserId) {
        BlockedUserEvent event = new BlockedUserEvent(userId, blockedUserId, Instant.now());
        blockedUserEventKafkaTemplate.send(blockedUsersTopic, event.userId() + ":" + event.blockedUserId(), event);
        log.info("BlockListService publish blocked user event");
    }
}