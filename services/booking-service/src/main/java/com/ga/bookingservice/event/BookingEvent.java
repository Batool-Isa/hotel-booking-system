package com.ga.bookingservice.event;

import com.ga.bookingservice.client.HotelSummary;
import com.ga.bookingservice.client.UserSummary;
import com.ga.bookingservice.model.Booking;
import com.ga.bookingservice.model.BookingGuest;
import com.ga.bookingservice.model.BookingRoom;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

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
    private String status;
    private Long userId;
    private Long hotelId;
    private String hotelName;
    private String hotelAddress;
    private String hotelCity;
    private String hotelCountry;
    private BigDecimal hotelLatitude;
    private BigDecimal hotelLongitude;
    private String guestName;
    private String guestEmail;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int adults;
    private int children;
    private String specialRequest;
    private BigDecimal totalAmount;
    private List<Room> rooms = new ArrayList<>();
    private LocalDateTime occurredAt;

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Room {
        private String roomNumber;
        private String roomTypeName;
        private BigDecimal pricePerNight;
        private List<Guest> guests = new ArrayList<>();
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Guest {
        private String name;
        private Integer age;
        private String guestType;
    }

    public static BookingEvent from(Booking b, String type, HotelSummary hotel, UserSummary user) {
        BookingEvent e = new BookingEvent();
        e.setEventId(UUID.randomUUID().toString());
        e.setType(type);
        e.setBookingId(b.getId());
        e.setBookingReference(b.getBookingReference());
        e.setStatus(b.getStatus() == null ? null : b.getStatus().name());
        e.setUserId(b.getUserId());
        e.setHotelId(b.getHotelId());
        if (hotel != null) {
            e.setHotelName(hotel.getName());
            e.setHotelAddress(hotel.getAddress());
            e.setHotelCity(hotel.getCity());
            e.setHotelCountry(hotel.getCountry());
            e.setHotelLatitude(hotel.getLatitude());
            e.setHotelLongitude(hotel.getLongitude());
        }
        e.setGuestName(user == null ? null : user.getFullName());
        e.setGuestEmail(user == null ? null : user.getEmail());
        e.setCheckIn(b.getCheckIn());
        e.setCheckOut(b.getCheckOut());
        e.setAdults(b.getAdults() == null ? 0 : b.getAdults());
        e.setChildren(b.getChildren() == null ? 0 : b.getChildren());
        e.setSpecialRequest(b.getSpecialRequest());
        e.setTotalAmount(b.getTotalAmount());
        e.setRooms(toRooms(b));
        e.setOccurredAt(LocalDateTime.now());
        return e;
    }

    private static List<Room> toRooms(Booking b) {
        List<Room> rooms = new ArrayList<>();
        try {
            List<BookingRoom> sorted = new ArrayList<>(b.getBookingRooms());
            sorted.sort(Comparator.comparing(BookingRoom::getId, Comparator.nullsLast(Comparator.naturalOrder())));
            for (BookingRoom br : sorted) {
                List<Guest> guests = new ArrayList<>();
                for (BookingGuest g : br.getGuests()) {
                    guests.add(new Guest(g.getName(), g.getAge(),
                            g.getGuestType() == null ? null : g.getGuestType().name()));
                }
                rooms.add(new Room(br.getRoomNumber(), br.getRoomTypeName(), br.getPricePerNight(), guests));
            }
        } catch (RuntimeException ignored) {
            return new ArrayList<>();
        }
        return rooms;
    }
}
