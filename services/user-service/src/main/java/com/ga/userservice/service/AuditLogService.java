package com.ga.userservice.service;

import com.ga.userservice.model.User;
import com.ga.userservice.model.AuditLog;
import com.ga.userservice.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogService {
    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Store audit log into db
     * @param user who did the action
     * @param action action that happens like canceled ,  confirmed , register etc.
     * @param entityType type of entity affected like Booking , Users .. etc
     * @param entityId id of affected row in entity
     * @param description human readable description of the log
     */
    public void log(User user, String action, String entityType, Long entityId, String description){
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setEntity_type(entityType);
        log.setEntity_id(entityId);
        log.setDescription(description);
        log.setAction(action);
        auditLogRepository.save(log);
    }
}
