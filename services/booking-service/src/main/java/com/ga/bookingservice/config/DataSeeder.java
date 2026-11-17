package com.ga.bookingservice.config;

import com.ga.bookingservice.model.Booking;
import com.ga.bookingservice.model.BookingGuest;
import com.ga.bookingservice.model.BookingRoom;
import com.ga.bookingservice.model.Review;
import com.ga.bookingservice.repository.BookingRepository;
import com.ga.bookingservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder {

    private static final int BOOKING_COUNT = 60;

    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final JdbcTemplate jdbcTemplate;

    private final Random random = new Random(42);
    private final Map<Long, List<LocalDate[]>> roomCalendar = new HashMap<>();

    private record SeedRoom(Long id, Long hotelId, String hotelName, String roomNumber, String roomTypeName,
                            BigDecimal pricePerNight, int maxAdults, int maxChildren,
                            int infantMaxAge, int childMaxAge) {
    }

    private static final String[] FIRST_NAMES = {
            "Sara", "Omar", "Fatima", "Hassan", "Maryam", "Ali", "Zainab", "Ahmed", "Reem", "Yousef",
            "Huda", "Tariq", "Aisha", "Jasim", "Dana", "Karim", "Noora", "Faisal", "Lulwa", "Hamad"
    };

    private static final String[] LAST_NAMES = {
            "Ahmed", "Khalid", "Al Zayani", "Jaffar", "Salman", "Hussain", "Mahmood", "Fakhro",
            "Nasser", "Darwish", "Abdulla", "Mansoor", "Rashid", "Saleh", "Mubarak", "Haddad"
    };

    private static final String[] SPECIAL_REQUESTS = {
            null, null, null, "Late check-in around 11pm", "High floor if possible", "Extra pillows please",
            "Baby cot needed", "Quiet room away from the elevator", "Celebrating an anniversary",
            "Early check-in if available"
    };

    private static final String[][] REVIEWS = {
            {"5", "Amazing stay, the staff were incredibly welcoming and the room was spotless."},
            {"5", "Perfect location and a great breakfast. We will definitely come back."},
            {"4", "Very comfortable room and friendly staff. The pool area was a highlight."},
            {"4", "Good value for money. Check-in was quick and easy."},
            {"4", "Lovely hotel, only wish the Wi-Fi was a little faster."},
            {"3", "Decent stay overall, but the room was smaller than expected."},
            {"3", "Nice view, though housekeeping was a bit slow."},
            {"5", "The kids loved it. Great family-friendly facilities."},
            {"2", "Air conditioning was noisy and it was hard to sleep."},
            {"4", "Clean, modern and close to everything we needed."}
    };

    @Bean
    CommandLineRunner seedBookings() {
        return args -> {
            if (bookingRepository.count() > 0) {
                log.info("Bookings already exist, skipping booking seed");
                return;
            }
            List<SeedRoom> rooms = loadRooms();
            List<Long> customerIds = loadCustomerIds();
            if (rooms.isEmpty() || customerIds.isEmpty()) {
                log.warn("Booking seed skipped: found {} active rooms and {} customers. "
                        + "Start hotel-service and user-service first, then restart booking-service.",
                        rooms.size(), customerIds.size());
                return;
            }
            int created = 0;
            int reviews = 0;
            for (int i = 0; i < BOOKING_COUNT; i++) {
                Booking booking = createBooking(rooms, customerIds);
                if (booking == null) {
                    continue;
                }
                created++;
                if (booking.getStatus() == Booking.Status.COMPLETED && random.nextInt(10) < 7) {
                    createReview(booking);
                    reviews++;
                }
            }
            log.info("Seeded {} bookings and {} reviews", created, reviews);
        };
    }

    private List<SeedRoom> loadRooms() {
        String sql = """
                SELECT r.id, r.hotel_id, h.name AS hotel_name, r.room_number, t.name AS type_name,
                       r.price_per_night, r.max_adults, r.max_children,
                       COALESCE(cp.infant_max_age, 2) AS infant_max_age,
                       COALESCE(cp.child_max_age, 12) AS child_max_age
                FROM rooms r
                JOIN hotels h ON h.id = r.hotel_id
                JOIN room_types t ON t.id = r.room_type_id
                LEFT JOIN child_policies cp ON cp.hotel_id = h.id
                WHERE r.status = 'ACTIVE' AND h.status = 'ACTIVE' AND r.price_per_night IS NOT NULL
                ORDER BY r.id
                """;
        try {
            return jdbcTemplate.query(sql, (rs, n) -> new SeedRoom(
                    rs.getLong("id"),
                    rs.getLong("hotel_id"),
                    rs.getString("hotel_name"),
                    rs.getString("room_number"),
                    rs.getString("type_name"),
                    rs.getBigDecimal("price_per_night"),
                    rs.getInt("max_adults"),
                    rs.getInt("max_children"),
                    rs.getInt("infant_max_age"),
                    rs.getInt("child_max_age")));
        } catch (DataAccessException e) {
            log.warn("Could not read rooms for booking seed: {}", e.getMostSpecificCause().getMessage());
            return List.of();
        }
    }

    private List<Long> loadCustomerIds() {
        String sql = """
                SELECT u.id FROM users u
                JOIN roles r ON r.id = u.role_id
                WHERE r.name = 'CUSTOMER'
                ORDER BY u.id
                """;
        try {
            return jdbcTemplate.queryForList(sql, Long.class);
        } catch (DataAccessException e) {
            log.warn("Could not read customers for booking seed: {}", e.getMostSpecificCause().getMessage());
            return List.of();
        }
    }

    private Booking createBooking(List<SeedRoom> rooms, List<Long> customerIds) {
        Booking.Status status = pickStatus();
        int nights = 1 + random.nextInt(5);
        LocalDate checkIn = pickCheckIn(status);
        LocalDate checkOut = checkIn.plusDays(nights);

        SeedRoom room = findFreeRoom(rooms, checkIn, checkOut);
        if (room == null) {
            return null;
        }
        if (status != Booking.Status.CANCELLED) {
            roomCalendar.computeIfAbsent(room.id(), k -> new ArrayList<>()).add(new LocalDate[]{checkIn, checkOut});
        }

        Booking booking = new Booking();
        booking.setBookingReference(referenceFor(room.hotelName()));
        booking.setHotelId(room.hotelId());
        booking.setUserId(customerIds.get(random.nextInt(customerIds.size())));
        booking.setCheckIn(checkIn);
        booking.setCheckOut(checkOut);
        booking.setStatus(status);
        booking.setSpecialRequest(SPECIAL_REQUESTS[random.nextInt(SPECIAL_REQUESTS.length)]);

        BookingRoom bookingRoom = new BookingRoom();
        bookingRoom.setBooking(booking);
        bookingRoom.setRoomId(room.id());
        bookingRoom.setRoomNumber(room.roomNumber());
        bookingRoom.setRoomTypeName(room.roomTypeName());
        bookingRoom.setPricePerNight(room.pricePerNight());

        int adults = 1 + random.nextInt(Math.max(1, room.maxAdults()));
        int children = room.maxChildren() > 0 ? random.nextInt(room.maxChildren() + 1) : 0;
        String familyName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        List<BookingGuest> guests = new ArrayList<>();
        for (int i = 0; i < adults; i++) {
            guests.add(guest(bookingRoom, familyName, 22 + random.nextInt(45), BookingGuest.GuestType.ADULT));
        }
        int childMinAge = room.infantMaxAge() + 1;
        int childAgeRange = Math.max(1, room.childMaxAge() - childMinAge + 1);
        for (int i = 0; i < children; i++) {
            guests.add(guest(bookingRoom, familyName, childMinAge + random.nextInt(childAgeRange),
                    BookingGuest.GuestType.CHILD));
        }
        bookingRoom.setGuests(guests);

        booking.setBookingRooms(new HashSet<>(Set.of(bookingRoom)));
        booking.setAdults(adults);
        booking.setChildren(children);
        booking.setTotalAmount(room.pricePerNight().multiply(BigDecimal.valueOf(nights)));

        booking = bookingRepository.save(booking);
        backdate(booking, checkIn);
        return booking;
    }

    private Booking.Status pickStatus() {
        int roll = random.nextInt(100);
        if (roll < 45) {
            return Booking.Status.COMPLETED;
        }
        if (roll < 85) {
            return Booking.Status.CONFIRMED;
        }
        return Booking.Status.CANCELLED;
    }

    private LocalDate pickCheckIn(Booking.Status status) {
        LocalDate today = LocalDate.now();
        return switch (status) {
            case COMPLETED -> today.minusDays(10 + random.nextInt(170));
            case CONFIRMED -> today.plusDays(3 + random.nextInt(90));
            default -> today.plusDays(random.nextInt(120) - 60);
        };
    }

    private SeedRoom findFreeRoom(List<SeedRoom> rooms, LocalDate checkIn, LocalDate checkOut) {
        for (int attempt = 0; attempt < 20; attempt++) {
            SeedRoom room = rooms.get(random.nextInt(rooms.size()));
            boolean overlaps = roomCalendar.getOrDefault(room.id(), List.of()).stream()
                    .anyMatch(range -> checkIn.isBefore(range[1]) && checkOut.isAfter(range[0]));
            if (!overlaps) {
                return room;
            }
        }
        return null;
    }

    private BookingGuest guest(BookingRoom room, String familyName, int age, BookingGuest.GuestType type) {
        BookingGuest guest = new BookingGuest();
        guest.setBookingRoom(room);
        guest.setName(FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " " + familyName);
        guest.setAge(age);
        guest.setGuestType(type);
        return guest;
    }

    private String referenceFor(String hotelName) {
        String prefix = (hotelName.replaceAll("[^A-Za-z]", "").toUpperCase() + "XXXX").substring(0, 4);
        String suffix = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        return prefix + suffix;
    }

    private void backdate(Booking booking, LocalDate checkIn) {
        LocalDateTime createdAt = checkIn.atTime(10, 0).minusDays(3 + random.nextInt(40));
        LocalDateTime now = LocalDateTime.now();
        if (createdAt.isAfter(now)) {
            createdAt = now.minus(random.nextInt(48), ChronoUnit.HOURS);
        }
        jdbcTemplate.update("UPDATE bookings SET created_at = ?, updated_at = ? WHERE id = ?",
                Timestamp.valueOf(createdAt), Timestamp.valueOf(createdAt), booking.getId());
    }

    private void createReview(Booking booking) {
        String[] pick = REVIEWS[random.nextInt(REVIEWS.length)];
        Review review = new Review();
        review.setBooking(booking);
        review.setRating(Double.parseDouble(pick[0]));
        review.setComment(pick[1]);
        review = reviewRepository.save(review);
        LocalDateTime reviewedAt = booking.getCheckOut().atTime(12, 0).plusDays(random.nextInt(5));
        if (reviewedAt.isAfter(LocalDateTime.now())) {
            reviewedAt = LocalDateTime.now();
        }
        jdbcTemplate.update("UPDATE reviews SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(reviewedAt), review.getId());
    }
}
