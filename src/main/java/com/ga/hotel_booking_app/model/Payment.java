package com.ga.hotel_booking_app.model;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private METHOD method;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private STATUS status = STATUS.PENDING;
    @Column(nullable = false)
    private String providerReference;
    @Column(nullable = false)
    private String idempotencyKey;
    @Column(length = 1000)
    private String sessionURL;
    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    @Column(length = 100)
    private String paymentIntent;
    @JsonIgnore
    @OneToOne(mappedBy = "payment")
    private Refund refund;

    public enum STATUS{
        PENDING,
        FAILED,
        SUCCEED,
        REFUNDED
    }
    public enum METHOD{
        CARD,
        FAILED,
        SUCCEED
    }
}
