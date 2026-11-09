package com.ga.hotelservice.service;

import com.ga.hotelservice.model.AuditLog;
import com.ga.hotelservice.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Store audit log into db
     * @param userId id of the user who did the action (from the JWT)
     * @param action action that happens like created, updated, deleted etc.
     * @param entityType type of entity affected like Hotel, Amenity .. etc
     * @param entityId id of affected row in entity
     * @param description human readable description of the log
     */
    public void log(Long userId, String action, String entityType, Long entityId, String description){
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setEntity_type(entityType);
        log.setEntity_id(entityId);
        log.setDescription(description);
        log.setAction(action);
        auditLogRepository.save(log);
    }
}
