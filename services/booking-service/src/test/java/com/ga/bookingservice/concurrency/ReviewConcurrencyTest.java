package com.ga.bookingservice.concurrency;

import com.ga.bookingservice.dto.request.ReviewRequest;
import com.ga.bookingservice.exception.custom.InformationExistException;
import com.ga.bookingservice.model.Booking;
import com.ga.bookingservice.model.Hotel;
import com.ga.bookingservice.model.User;
import com.ga.bookingservice.repository.BookingRepository;
import com.ga.bookingservice.repository.HotelRepository;
import com.ga.bookingservice.repository.ReviewRepository;
import com.ga.bookingservice.repository.UserRepository;
import com.ga.bookingservice.service.AuditLogService;
import com.ga.bookingservice.service.EmailService;
import com.ga.bookingservice.service.NotificationService;
import com.ga.bookingservice.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class ReviewConcurrencyTest {
    @Autowired
    ReviewService reviewService;
    @Autowired
    ReviewRepository reviewRepository;
    @Autowired
    BookingRepository bookingRepository;
    @Autowired
    HotelRepository hotelRepository;
    @Autowired
    UserRepository userRepository;
    @MockitoBean
    EmailService emailService;
    @MockitoBean
    NotificationService notificationService;
    @MockitoBean
    AuditLogService auditLogService;
    private String uniqueId;
    private Hotel hotel;
    private final List<User> users = new ArrayList<>();

    @BeforeEach
    void setup() {
        uniqueId = UUID.randomUUID().toString().substring(0, 8);
        hotel = new Hotel();
        hotel.setName("Review Hotel " + uniqueId);
        hotel.setDescription("test");
        hotel.setAddress("Street 1");
        hotel.setCity("Manama");
        hotel.setCountry("Bahrain");
        hotel.setPhone("12345678");
        hotel.setStatus(Hotel.Status.ACTIVE);
        hotel = hotelRepository.save(hotel);

        for (int i = 0; i < 3; i++) {
            User u = new User();
            u.setUsername("user" + i + "-" + uniqueId);
            u.setEmail("user" + i + "-" + uniqueId + "@test.com");
            u.setPassword("password");
            u.setStatus(User.Status.ACTIVE);
            users.add(userRepository.save(u));
        }
    }

    @Test
    void FiveRequestsReviewingSameBookingSaveOnlyOneReview() throws Exception {
        User owner = users.get(0);
        Booking booking = createCompletedBooking(owner, 0);
        long reviewsBefore = reviewRepository.count();
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        for (int i = 0; i < 5; i++) {
            executor.execute(() -> {
                try {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(owner.getEmail(), null));
                    startGate.await();
                    reviewService.postReview(booking.getId(), reviewRequest());
                    success.incrementAndGet();
                } catch (InformationExistException | DataIntegrityViolationException e) {
                    rejected.incrementAndGet();
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
        }
        startGate.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        assertEquals(0, errors.get(), "unexpected errors");
        assertEquals(1, success.get(), "exactly one review must succeed");
        assertEquals(4, rejected.get(), "all others must be rejected as duplicate");
        assertEquals(reviewsBefore + 1, reviewRepository.count(), "only one review row in the database");
    }

    @Test
    void ThreeUsersReviewingTheirOwnBookingsAllSucceed() throws Exception {
        List<Booking> bookings = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            bookings.add(createCompletedBooking(users.get(i), i));
        }
        long reviewsBefore = reviewRepository.count();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        for (int i = 0; i < 3; i++) {
            String email = users.get(i).getEmail();
            Long bookingId = bookings.get(i).getId();
            executor.execute(() -> {
                try {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(email, null));
                    startGate.await();
                    reviewService.postReview(bookingId, reviewRequest());
                    success.incrementAndGet();
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
        }

        startGate.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        assertEquals(0, errors.get(), "unexpected errors");
        assertEquals(3, success.get(), "all three reviews must succeed");
        assertEquals(reviewsBefore + 3, reviewRepository.count(), "three review rows in the database");
    }
    private Booking createCompletedBooking(User owner, int offset) {
        Booking booking = new Booking();
        booking.setBookingReference("RV-" + uniqueId + "-" + offset);
        booking.setCheckIn(LocalDate.now().minusDays(5 + offset));
        booking.setCheckOut(LocalDate.now().minusDays(3 + offset));
        booking.setAdults(1);
        booking.setChildren(0);
        booking.setTotalAmount(new BigDecimal("100.00"));
        booking.setStatus(Booking.Status.COMPLETED);
        booking.setHotel(hotel);
        booking.setUser(owner);
        return bookingRepository.save(booking);
    }

    private ReviewRequest reviewRequest() {
        ReviewRequest request = new ReviewRequest();
        ReflectionTestUtils.setField(request, "rating", 5.0);
        ReflectionTestUtils.setField(request, "comment", "Great stay");
        return request;
    }
}
