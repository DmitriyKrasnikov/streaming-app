package com.krasnikov.streaming.streams;

import com.krasnikov.streaming.model.ChatMessage;
import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class CensorshipService {

    private final Set<String> forbiddenWords = ConcurrentHashMap.newKeySet();
    private final KafkaTemplate<String, String> stringKafkaTemplate;

    @Value("${application.topic.forbidden-words}")
    private String forbiddenWordsTopic;
    @Value("${application.initial-forbidden-words:}")
    private List<String> initialWords;

    @Autowired
    public CensorshipService(
            @Qualifier("stringKafkaTemplate") KafkaTemplate<String, String> stringKafkaTemplate
    ) {
        this.stringKafkaTemplate = stringKafkaTemplate;
    }

    @PostConstruct
    public void init() {
        initialWords.stream()
                .map(String::toLowerCase)
                .forEach(word -> {
                    forbiddenWords.add(word);
                    stringKafkaTemplate.send(forbiddenWordsTopic, word, "ADD");
                });
    }

    public ChatMessage censor(ChatMessage msg) {
        String content = msg.content();
        for (String word : forbiddenWords) {
            String mask = "*".repeat(word.length());
            Pattern p = Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE);
            content = p.matcher(content).replaceAll(mask);
        }
        return new ChatMessage(
                msg.senderId(),
                msg.recipientId(),
                content,
                msg.timestamp()
        );
    }

    @KafkaListener(
            topics = "${application.topic.forbidden-words}",
            groupId = "censorship-service"
    )
    public void handleWordUpdate(ConsumerRecord<String, String> record) {
        String word = record.key();
        String action = record.value();

        if (word == null || word.isBlank()) return;
        word = word.trim().toLowerCase();

        if (action == null) return;
        action = action.trim().toUpperCase();

        switch (action) {
            case "ADD" -> forbiddenWords.add(word);
            case "REMOVE" -> forbiddenWords.remove(word);
            default -> {
            }
        }
    }

    public void publishUpdate(String action, String word) {
        String normalizedWord = word.trim().toLowerCase();
        String normalizedAction = action.trim().toUpperCase();

        stringKafkaTemplate.send(
                forbiddenWordsTopic,
                normalizedWord,
                normalizedAction
        );
    }

    public Set<String> getForbiddenWords() {
        return Set.copyOf(forbiddenWords);
    }
}