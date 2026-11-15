package com.ga.bookingservice.service;

import com.ga.bookingservice.event.AuditLogEvent;
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
     * Store audit log into db
     * @param userId unique id of the user who did the action
     * @param action action that happens like canceled ,  confirmed , register etc.
     * @param entityType type of entity affected like Booking , Users .. etc
     * @param entityId id of affected row in entity
     * @param description human readable description of the log
     */
    public void log(Long userId, String action, String entityType, Long entityId, String description){
        AuditLogEvent log = new AuditLogEvent();
        log.setUserId(userId);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDescription(description);
        log.setAction(action);
        log.setEventId(UUID.randomUUID().toString());
        String source = "booking-service";
        log.setSourceService(source);
        log.setOccurredAt(LocalDateTime.now());
        eventPublisher.publishEvent(log);
    }
}
