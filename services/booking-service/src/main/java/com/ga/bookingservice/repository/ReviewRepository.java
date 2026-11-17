package com.ga.bookingservice.repository;

import com.ga.bookingservice.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
   boolean existsByBookingId(Long id);
 @Query("SELECT r FROM Review r JOIN FETCH r.booking b WHERE b.hotelId = :hotelId ORDER BY r.createdAt DESC")
   List<Review> findHotelReviews(
           @Param("hotelId") Long hotelId);
   @Query("SELECT AVG(r.rating) FROM Review r")
   Double averageRating();
}
