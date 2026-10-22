package com.ga.hotel_booking_app.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="refunds")
@Data
public class Refund {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(nullable = true)
    private String reason;
    @CreationTimestamp
    @Column
    private LocalDateTime createdAt;
}
