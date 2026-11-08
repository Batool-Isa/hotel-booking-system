package com.ga.hotel_booking_app.repository;

import com.ga.hotel_booking_app.model.EmailVerificationToken;
import com.ga.hotel_booking_app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends
        JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);


}
