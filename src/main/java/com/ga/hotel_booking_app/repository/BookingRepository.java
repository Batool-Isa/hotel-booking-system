package com.ga.hotel_booking_app.repository;

import com.ga.hotel_booking_app.model.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;



@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByStatus(Booking.Status status);
    List<Booking> findByUserId(Long userId);
    Page<Booking> findByUserId(Long userId, Pageable pageable);
    List<Booking> findByHotelId(Long hotelId);
    List<Booking> findByHotelIdAndStatus(Long hotelId, Booking.Status status);

    @Query("""
                SELECT b
                FROM Booking b
                JOIN b.bookingRooms br
                WHERE br.room.id = :roomId
                AND b.status = :status
                AND b.checkIn < :checkOut
                AND b.checkOut > :checkIn
            """)
    List<Booking> findOverlappingBookings(
            @Param("roomId") Long roomId,
            @Param("status") Booking.Status status,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );
}
