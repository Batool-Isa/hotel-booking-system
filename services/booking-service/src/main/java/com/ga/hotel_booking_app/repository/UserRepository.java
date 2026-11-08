package com.ga.hotel_booking_app.repository;

import com.ga.hotel_booking_app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    User findUserByEmail(String email);
    @Query("SELECT u.role.name, COUNT(u) FROM User u GROUP BY u.role.name")
    List<Object[]> countGroupByRole();
    long countByCreatedAtGreaterThanEqual(LocalDateTime from);
}
