package com.ga.userservice.kafka;

import com.ga.userservice.config.KafkaTopicConfig;
import com.ga.userservice.event.AuditLogEvent;
import com.ga.userservice.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class AuditLogEventConsumer {
    @Autowired
    AuditLogService auditLogService;
    private static final Logger logger = LoggerFactory.getLogger(AuditLogEventConsumer.class);
    private final JsonMapper jsonMapper;

    @KafkaListener(topics = KafkaTopicConfig.AUDIT_TOPIC, groupId = "user-service-audit")
    public void onAuditLogEvent(String json,
                               @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                               @Header(KafkaHeaders.OFFSET) long offset) {
        AuditLogEvent event = jsonMapper.readValue(json, AuditLogEvent.class);
        logger.info("Received audit {} for {} (userId {}, entityId {}, entityType {}) [partition {}, offset {}]",
                event.getAction(), event.getSourceService(), event.getUserId(),
                event.getEntityId(), event.getEntityType(), partition, offset);
        auditLogService.saveFromEvent(event);
    }
}
