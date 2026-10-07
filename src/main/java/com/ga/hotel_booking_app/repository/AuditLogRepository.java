package com.ga.hotel_booking_app.repository;

import com.ga.hotel_booking_app.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}