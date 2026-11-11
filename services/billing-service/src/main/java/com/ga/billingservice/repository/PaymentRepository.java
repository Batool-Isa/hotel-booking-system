package com.ga.billingservice.repository;

import com.ga.bookingservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByIdempotencyKey(String key);
    Payment findByProviderReference(String sessionId);
}