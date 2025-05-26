package com.krasnikov.streaming.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * ChatMessage — модель входящего или выходящего сообщения в чате.
 *
 * @param senderId    Уникальный идентификатор отправителя
 * @param recipientId Уникальный идентификатор получателя
 * @param content     Текст сообщения
 * @param timestamp   Момент времени отправки (ISO-8601)
 */
public record ChatMessage(String senderId, String recipientId, String content, Instant timestamp) {

    @JsonCreator
    public ChatMessage(
            @JsonProperty("senderId") String senderId,
            @JsonProperty("recipientId") String recipientId,
            @JsonProperty("content") String content,
            @JsonProperty("timestamp") Instant timestamp
    ) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.content = content;
        this.timestamp = timestamp;
    }
}
