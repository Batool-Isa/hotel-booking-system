package com.ga.notificationservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserEvent(
        String eventId,
        String type,
        Long userId,
        String email,
        String name,
        String link,
        LocalDateTime expiresAt,
        LocalDateTime occurredAt
) {
}
