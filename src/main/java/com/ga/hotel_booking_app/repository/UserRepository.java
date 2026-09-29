package com.ga.hotel_booking_app.repository;

import com.ga.hotel_booking_app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    User findUserByEmail(String email);

}
