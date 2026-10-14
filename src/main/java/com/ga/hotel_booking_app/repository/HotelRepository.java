package com.ga.hotel_booking_app.repository;

import com.ga.hotel_booking_app.model.Hotel;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {
    boolean existsByName(String name);

    List<Hotel> findByStatus(Hotel.Status status);

    Page<Hotel> findByStatus(Hotel.Status status, Pageable pageable);

    List<Hotel> findByManagers_Id(Long managerId);

    Page<Hotel> findByManagers_Id(Long managerId, Pageable pageable);

    List<Hotel> findByNameContainingIgnoreCaseAndStatus(String name, Hotel.Status status);

    Hotel findByName(String name);

    @Query("SELECT h FROM Hotel h WHERE h.status = 'ACTIVE' " +
            "AND (CAST(:search AS string) IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
            "AND (CAST(:name AS string) IS NULL OR LOWER(h.name) = LOWER(CAST(:name AS string))) " +
            "AND (CAST(:city AS string) IS NULL OR LOWER(h.city) = LOWER(CAST(:city AS string))) " +
            "AND (CAST(:country AS string) IS NULL OR LOWER(h.country) = LOWER(CAST(:country AS string)))")
    Page<Hotel> findFilteredHotels(
            @Param("search") String search,
            @Param("name") String name,
            @Param("city") String city,
            @Param("country") String country,
            Pageable pageable);

    @Query("SELECT h FROM Hotel h WHERE h.status = 'ACTIVE' " +
            "AND (CAST(:city AS string) IS NULL OR LOWER(h.city) = LOWER(CAST(:city AS string))) " +
            "AND EXISTS (SELECT r FROM Room r WHERE r.hotel = h AND r.status = 'ACTIVE' " +
            "  AND r.maxAdults >= :adults AND r.maxChildren >= :children " +
            "  AND r.maxOccupancy >= :adults + :children " +
            "  AND NOT EXISTS (SELECT br FROM BookingRoom br WHERE br.room = r " +
            "    AND br.booking.status = 'CONFIRMED' " +
            "    AND br.booking.checkIn < :checkOut AND br.booking.checkOut > :checkIn))")
    Page<Hotel> findHotelsWithFreeRooms(
            @Param("city") String city,
            @Param("adults") int adults,
            @Param("children") int children,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            Pageable pageable);
}
