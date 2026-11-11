package com.ga.bookingservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "booking_rooms")
public class BookingRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    // copies from hotel-service saved at booking time (like pricePerNight), so showing a booking
    // doesn't need a call to hotel-service. null for bookings made before these columns existed.
    @Column(length = 20)
    private String roomNumber;

    @Column(length = 100)
    private String roomTypeName;

    @OneToMany(mappedBy = "bookingRoom", cascade = CascadeType.ALL)
    private List<BookingGuest> guests = new ArrayList<>();
}