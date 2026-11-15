package com.ga.hotelservice.kafka;

import com.ga.hotelservice.config.KafkaTopicConfig;
import com.ga.hotelservice.event.AuditLogEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class AuditLogEventProducer {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogEventProducer.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public void publish(AuditLogEvent event) {
        try {
            String json = jsonMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopicConfig.AUDIT_TOPIC, event.getEventId(), json)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            logger.error("Could not publish audit for action {}, event id: {}, user id: {}, message: {}",
                                    event.getAction(), event.getEventId(), event.getUserId(), error.getMessage());
                        } else {
                            logger.info("Published audit for action {} for user {} to partition {}, offset {}",
                                    event.getAction(), event.getUserId(),
                                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            logger.error("Could not publish audit for action {}, event id: {}, user id: {}, message: {}",
                    event.getAction(), event.getEventId(), event.getUserId(), e.getMessage());
        }
    }
}