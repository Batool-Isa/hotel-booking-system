package com.ga.hotel_booking_app.model;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "booking_guests")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BookingGuest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GuestType guestType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_room_id", nullable = false)
    private BookingRoom bookingRoom;


    public enum GuestType {
        ADULT,
        CHILD,
        INFANT
    }
}