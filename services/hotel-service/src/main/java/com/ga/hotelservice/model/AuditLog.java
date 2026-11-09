package com.ga.hotelservice.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String action;
    @Column
    private String entity_type;
    @Column
    private Long entity_id;
    @Column
    private String description;
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    // id of the user (from the JWT) who did the action
    @Column(name = "user_id")
    private Long userId;
}
