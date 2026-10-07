package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.model.AuditLog;
import com.ga.hotel_booking_app.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogService {
    @Autowired
    private AuditLogRepository auditLogRepository;

    public void log(Long userId, String action, String entityType, Long entityId, String description){
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setEntity_type(entityType);
        log.setEntity_id(entityId);
        log.setDescription(description);
        log.setAction(action);
    }
}
