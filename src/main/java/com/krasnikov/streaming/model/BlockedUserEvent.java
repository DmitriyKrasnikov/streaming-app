package com.krasnikov.streaming.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * BlockedUserEvent — событие добавления пользователя в блок-лист другого пользователя.
 *
 * @param userId        Идентификатор того, кто блокирует
 * @param blockedUserId Идентификатор того, кого блокируют
 * @param blockedAt     Время блокировки (ISO-8601)
 */
public record BlockedUserEvent(String userId, String blockedUserId, Instant blockedAt) {

    @JsonCreator
    public BlockedUserEvent(
            @JsonProperty("userId") String userId,
            @JsonProperty("blockedUserId") String blockedUserId,
            @JsonProperty("blockedAt") Instant blockedAt
    ) {
        this.userId = userId;
        this.blockedUserId = blockedUserId;
        this.blockedAt = blockedAt;
    }
}