package com.ga.userservice.kafka;

import com.ga.userservice.config.KafkaTopicConfig;
import com.ga.userservice.event.UserEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class UserEventProducer {
    private static final Logger logger = LoggerFactory.getLogger(UserEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public void publish(UserEvent event) {
        try {
            String json = jsonMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopicConfig.USER_TOPIC, String.valueOf(event.getUserId()), json)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            logger.error("Could not publish {} for user {}: {}",
                                    event.getType(), event.getUserId(), error.getMessage());
                        } else {
                            logger.info("Published {} for user {} to partition {}, offset {}",
                                    event.getType(), event.getUserId(),
                                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            logger.error("Could not publish {} for user {}: {}", event.getType(), event.getUserId(), e.getMessage());
        }
    }
}
