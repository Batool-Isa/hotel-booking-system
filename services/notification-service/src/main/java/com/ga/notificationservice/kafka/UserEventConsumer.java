package com.ga.notificationservice.kafka;

import com.ga.notificationservice.event.UserEvent;
import com.ga.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class UserEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(UserEventConsumer.class);
    private final JsonMapper jsonMapper;
    private final EmailService emailService;

    @KafkaListener(topics = "user-events", groupId = "notification-service")
    public void onUserEvent(String json,
                            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                            @Header(KafkaHeaders.OFFSET) long offset) {
        UserEvent event = jsonMapper.readValue(json, UserEvent.class);
        logger.info("Received {} for user {} ({}) [partition {}, offset {}]",
                event.type(), event.userId(), event.email(), partition, offset);

        switch (event.type()) {
            case "USER_REGISTERED" -> emailService.sendVerificationEmail(event);
            case "PASSWORD_RESET_REQUESTED" -> emailService.sendResetLink(event);
            default -> logger.warn("Unknown user event type {}", event.type());
        }
    }
}
