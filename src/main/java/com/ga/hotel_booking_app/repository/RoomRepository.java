package com.ga.hotel_booking_app.repository;

import com.ga.hotel_booking_app.model.Hotel;
import com.ga.hotel_booking_app.model.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByStatus(Room.Status status);

    Optional<Room> findByIdAndStatus(Long id, Room.Status status);

    List<Room> findByHotelId(Long hotelId);

    Page<Room> findByHotelId(Long hotelId, Pageable pageable);

    Page<Room> findByHotelIdAndStatus(Long hotelId, Room.Status status, Pageable pageable);
    boolean existsByHotelIdAndRoomNumber(Long hotelId, String roomNumber);
    List<Room> findByHotel(Hotel hotel);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);
    @Query("SELECT r FROM Room r WHERE r.hotel.id IN :hotelIds AND r.status = 'ACTIVE' " +
            "AND r.maxAdults >= :adults AND r.maxChildren >= :children " +
            "AND r.maxOccupancy >= :adults + :children " +
            "AND NOT EXISTS (SELECT br FROM BookingRoom br WHERE br.room = r " +
            "  AND br.booking.status = 'CONFIRMED' " +
            "  AND br.booking.checkIn < :checkOut AND br.booking.checkOut > :checkIn)")
    List<Room> findFreeRooms(
            @Param("hotelIds") List<Long> hotelIds,
            @Param("adults") int adults,
            @Param("children") int children,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut);
}
