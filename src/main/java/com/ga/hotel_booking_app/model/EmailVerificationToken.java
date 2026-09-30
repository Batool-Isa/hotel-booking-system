package com.ga.hotel_booking_app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String token;
    @CreationTimestamp
    @Column
    private LocalDateTime createdAt;
    @Column
    private LocalDateTime usedAt;
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name="user_id")
    private User user;
}
