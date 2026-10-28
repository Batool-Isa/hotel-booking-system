package com.ga.hotel_booking_app.repository;

import com.ga.hotel_booking_app.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
   boolean existsByBookingId(Long id);
   @Query("SELECT r FROM Review r JOIN FETCH r.booking b JOIN FETCH b.user WHERE b.hotel.id = :hotelId ORDER BY r.createdAt DESC"
           )
   List<Review> findHotelReviews(
           @Param("hotelId") Long hotelId);
}
