package com.ga.userservice.service;

import com.ga.userservice.event.AuditLogEvent;
import com.ga.userservice.model.User;
import com.ga.userservice.model.AuditLog;
import com.ga.userservice.repository.AuditLogRepository;
import com.ga.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    /**
     * Store audit log into db
     *
     * @param user        who did the action
     * @param action      action that happens like canceled ,  confirmed , register etc.
     * @param entityType  type of entity affected like Booking , Users .. etc
     * @param entityId    id of affected row in entity
     * @param description human readable description of the log
     */
    public void log(User user, String action, String entityType, Long entityId, String description) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setEntity_type(entityType);
        log.setEntity_id(entityId);
        log.setDescription(description);
        log.setAction(action);
        auditLogRepository.save(log);
    }

    /**
     * Save events that coming from other service using kafka
     * @param event event object that holds all event info
     * @return true if saved successfully in db
     */
    public boolean saveFromEvent(AuditLogEvent event) {
        if (auditLogRepository.existsByEventId(event.getEventId())) {
            logger.info("Audit event {} already saved, skipping it", event.getEventId());
            return false;
        }

        User user = event.getUserId() == null ? null : userRepository.findById(event.getUserId()).orElse(null);

        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(event.getAction());
        log.setEntity_type(event.getEntityType());
        log.setEntity_id(event.getEntityId());
        log.setDescription(event.getDescription());
        log.setOccurredAt(event.getOccurredAt());
        log.setSourceService(event.getSourceService());
        log.setEventId(event.getEventId());
        try {
            auditLogRepository.save(log);
            return true;
        } catch (DataIntegrityViolationException e) {
            // 2 copies of same event arrived same time , unique event id constrains reject the 2nd one
            logger.info("Audit event {} was saved by another thread, skipping this one", event.getEventId());
            return false;
        }
    }
}