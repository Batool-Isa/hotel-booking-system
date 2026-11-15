package com.ga.hotelservice.service;

import com.ga.hotelservice.event.AuditLogEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuditLogService {
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    /**
     * publish audit log to kafka
     * @param userId id of the user who did the action (from the JWT)
     * @param action action that happens like created, updated, deleted etc.
     * @param entityType type of entity affected like Hotel, Amenity .. etc
     * @param entityId id of affected row in entity
     * @param description human-readable description of the log
     */
    public void log(Long userId, String action, String entityType, Long entityId, String description){
        AuditLogEvent log = new AuditLogEvent();
        log.setUserId(userId);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDescription(description);
        log.setAction(action);
        log.setEventId(UUID.randomUUID().toString());
        String source = "hotel-service";
        log.setSourceService(source);
        log.setOccurredAt(LocalDateTime.now());
        eventPublisher.publishEvent(log);
    }
}
