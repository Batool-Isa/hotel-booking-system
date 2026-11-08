package com.ga.hotel_booking_app.repository;

import com.ga.hotel_booking_app.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByIdempotencyKey(String key);
    Payment findByProviderReference(String sessionId);
}