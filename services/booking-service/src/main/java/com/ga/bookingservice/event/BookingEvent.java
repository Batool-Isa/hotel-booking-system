package com.ga.bookingservice.event;

import com.ga.bookingservice.client.HotelSummary;
import com.ga.bookingservice.client.UserSummary;
import com.ga.bookingservice.model.Booking;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// the message sent to Kafka (as JSON). notification-service has its own copy with the same field names
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BookingEvent {
    private String eventId;
    private String type;
    private Long bookingId;
    private String bookingReference;
    private Long userId;
    private Long hotelId;
    private String hotelName;
    private String guestName;
    private String guestEmail;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private BigDecimal totalAmount;
    private LocalDateTime occurredAt;

    // hotel / user come from hotel-service and user-service. they can be null if that
    // service was down: the event is still sent, just without those fields
    public static BookingEvent from(Booking b, String type, HotelSummary hotel, UserSummary user) {
        BookingEvent e = new BookingEvent();
        e.setEventId(UUID.randomUUID().toString());
        e.setType(type);
        e.setBookingId(b.getId());
        e.setBookingReference(b.getBookingReference());
        e.setUserId(b.getUserId());
        e.setHotelId(b.getHotelId());
        e.setHotelName(hotel == null ? null : hotel.getName());
        e.setGuestName(user == null ? null : user.getFullName());
        e.setGuestEmail(user == null ? null : user.getEmail());
        e.setCheckIn(b.getCheckIn());
        e.setCheckOut(b.getCheckOut());
        e.setTotalAmount(b.getTotalAmount());
        e.setOccurredAt(LocalDateTime.now());
        return e;
    }
}
