package com.krasnikov.streaming.testing;

import com.krasnikov.streaming.model.ChatMessage;
import com.krasnikov.streaming.streams.BlockListService;
import com.krasnikov.streaming.streams.CensorshipService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Демонстрационный прогон тестовых сценариев для проверки функционала
 */
@Slf4j
@Component
public class StreamingDemoRunner {

    private final KafkaTemplate<String, ChatMessage> chatMessageKafkaTemplate;
    private final BlockListService blockListService;
    private final CensorshipService censorshipService;
    private final TaskScheduler taskScheduler;
    private final CountDownLatch completionLatch = new CountDownLatch(2);

    @Autowired
    public StreamingDemoRunner(
            @Qualifier("chatMessageKafkaTemplate") KafkaTemplate<String, ChatMessage> chatMessageKafkaTemplate,
            BlockListService blockListService,
            CensorshipService censorshipService,
            TaskScheduler taskScheduler
    ) {
        this.chatMessageKafkaTemplate = chatMessageKafkaTemplate;
        this.blockListService = blockListService;
        this.censorshipService = censorshipService;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    public void scheduleDemo() {
        taskScheduler.schedule(
                () -> {
                    try {
                        runDemo();
                    } catch (InterruptedException e) {
                        log.error("Demo interrupted", e);
                    }
                },
                Instant.now().plusSeconds(30)
        );
    }

    public void runDemo() throws InterruptedException {
        log.info("\n\n=== Starting streaming demo ===\n");

        // 1. Базовый сценарий
        sendTestMessage("user1", "user2", "Hello World!");
        Thread.sleep(10000);

        // 2. Тест блокировки пользователя
        blockListService.publishBlockEvent("user2", "user1");
        Thread.sleep(10000);
        //Это сообщение не будет отражаться в топике filtered-messages
        sendTestMessage("user1", "user2", "Try to block me");

        // 3. Тест цензуры
        censorshipService.publishUpdate("ADD", "secret");
        //Это сообщение придёт с цензурой на слово secret
        sendTestMessage("user3", "user4", "This is secret information");

        // Ожидаем обработки всех сообщений
        if (completionLatch.await(30, TimeUnit.SECONDS)) {
            log.info("\n=== All test messages processed ===");
        } else {
            log.warn("\n=== Some messages were not processed ===");
        }
    }

    private void sendTestMessage(String sender, String recipient, String text) {
        ChatMessage message = new ChatMessage(
                sender,
                recipient,
                text,
                Instant.now()
        );
        chatMessageKafkaTemplate.send("messages", message);
        log.info("Sent test message: { sender: {}, recipient: {}, text: '{}' }",
                sender, recipient, text);
        chatMessageKafkaTemplate.flush();
    }

    @KafkaListener(
            topics = "${application.topic.filtered-messages}",
            groupId = "demo-consumer",
            containerFactory = "chatMessageListenerFactory"
    )
    public void handleFilteredMessage(ChatMessage message) {
        log.info("Received filtered message: {}", formatMessage(message));
        completionLatch.countDown();
    }

    private String formatMessage(ChatMessage msg) {
        return String.format(
                "{ sender: %s, recipient: %s, text: '%s', time: %s }",
                msg.senderId(),
                msg.recipientId(),
                msg.content(),
                msg.timestamp()
        );
    }
}