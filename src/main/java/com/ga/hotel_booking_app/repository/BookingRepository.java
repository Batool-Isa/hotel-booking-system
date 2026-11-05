package com.ga.hotel_booking_app.repository;

import com.ga.hotel_booking_app.model.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;



@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {
    List<Booking> findByStatus(Booking.Status status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);
    List<Booking> findByUserId(Long userId);
    Page<Booking> findByUserId(Long userId, Pageable pageable);
    List<Booking> findByHotelId(Long hotelId);
    List<Booking> findByHotelIdAndStatus(Long hotelId, Booking.Status status);
    boolean existsByBookingReference(String refrence);
    @Query("""
                SELECT b
                FROM Booking b
                JOIN b.bookingRooms br
                WHERE br.room.id = :roomId
                AND ( b.status = com.ga.hotel_booking_app.model.Booking.Status.CONFIRMED OR 
                            (b.status = com.ga.hotel_booking_app.model.Booking.Status.PENDING
                                        AND b.holdExpiresAt > :now))
                AND b.checkIn < :checkOut
                AND b.checkOut > :checkIn
            """)
    List<Booking> findOverlappingBookings(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("now") LocalDateTime now
    );
    @Query("""
                SELECT COUNT(b) > 0
                FROM Booking b
                JOIN b.bookingRooms br
                WHERE br.room.id = :roomId
                AND b.status IN (com.ga.hotel_booking_app.model.Booking.Status.PENDING,
                                 com.ga.hotel_booking_app.model.Booking.Status.CONFIRMED)
                AND b.checkOut > :today
            """)
    boolean existsUpcomingBookingForRoom(@Param("roomId") Long roomId, @Param("today") LocalDate today);
    List<Booking> findByStatusAndHoldExpiresAtBefore(Booking.Status status, LocalDateTime dateTime);
    @Query("SELECT b.status, COUNT(b) FROM Booking b GROUP BY b.status")
    List<Object[]> countGroupByStatus();
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime to);
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.status IN :statuses")
    BigDecimal sumRevenue(@Param("statuses") List<Booking.Status> statuses);
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.status IN :statuses AND b.createdAt >= :from AND b.createdAt < :to")
    BigDecimal sumRevenueBetween(@Param("statuses") List<Booking.Status> statuses, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    @Query("SELECT CAST(b.createdAt AS LocalDate), COUNT(b), COALESCE(SUM(CASE WHEN b.status IN :statuses THEN b.totalAmount ELSE 0 END), 0) FROM Booking b WHERE b.createdAt >= :from GROUP BY CAST(b.createdAt AS LocalDate)")
    List<Object[]> dailyTotalsSince(@Param("statuses") List<Booking.Status> statuses, @Param("from") LocalDateTime from);
    @Query("SELECT b.hotel.id, b.hotel.name, COUNT(b), SUM(b.totalAmount) FROM Booking b WHERE b.status IN :statuses GROUP BY b.hotel.id, b.hotel.name ORDER BY SUM(b.totalAmount) DESC")
    List<Object[]> topHotelsByRevenue(@Param("statuses") List<Booking.Status> statuses, Pageable pageable);
}
