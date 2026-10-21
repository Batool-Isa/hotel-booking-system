package com.ga.hotel_booking_app.model;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@ToString(exclude = "booking")
@Data
public class Payment {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "bookingId")
    private Booking booking;
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(nullable = false)
    private String method;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private STATUS status = STATUS.PENDING;
    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum STATUS{
        PENDING,
        FAILED,
        SUCCEED
    }
}
