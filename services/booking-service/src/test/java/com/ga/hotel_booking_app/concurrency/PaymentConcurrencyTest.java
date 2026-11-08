package com.ga.hotel_booking_app.concurrency;

import com.ga.hotel_booking_app.model.*;
import com.ga.hotel_booking_app.repository.*;
import com.ga.hotel_booking_app.service.*;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class PaymentConcurrencyTest {
    @Autowired
    PaymentService paymentService;
    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    BookingRepository bookingRepository;
    @Autowired
    HotelRepository hotelRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    TransactionTemplate transactionTemplate;
    @MockitoBean
    StripeGateway stripeGateway;
    @MockitoBean
    EmailService emailService;
    @MockitoBean
    NotificationService notificationService;
    @Autowired
    BookingService bookingService;
    @Autowired
    RoleRepository roleRepository;
    @Value("${stripe.webhook.secret}")
    String webhookSecret;

    private Hotel hotel;
    private String uniqueId;
    private final List<User> users = new ArrayList<>();

    @BeforeEach
    void setup() throws Exception {
        uniqueId = UUID.randomUUID().toString().substring(0, 8);
        hotel = new Hotel();
        hotel.setName("Pay Hotel " + uniqueId);
        hotel.setDescription("test");
        hotel.setAddress("Street 1");
        hotel.setCity("Manama");
        hotel.setCountry("Bahrain");
        hotel.setPhone("12345678");
        hotel.setStatus(Hotel.Status.ACTIVE);
        hotel = hotelRepository.save(hotel);

        for (int i = 0; i < 5; i++) {
            User u = new User();
            u.setUsername("payer" + i + "-" + uniqueId);
            u.setEmail("payer" + i + "-" + uniqueId + "@test.com");
            u.setPassword("not-used");
            u.setStatus(User.Status.ACTIVE);
            users.add(userRepository.save(u));
        }

        AtomicInteger sessionCounter = new AtomicInteger();
        when(stripeGateway.createCheckoutSession(any())).thenAnswer(invocation -> {
            int n = sessionCounter.incrementAndGet();
            Session session = mock(Session.class);
            when(session.getId()).thenReturn("cs_" + uniqueId + "_" + n);
            when(session.getUrl()).thenReturn("https://stripe.test/" + uniqueId + "/" + n);
            return session;
        });
    }

    @Test
    void FiveRequestsWithSameIdempotencyKeyCreateOnlyOnePayment() throws Exception {
        User owner = users.get(0);
        Booking booking = createPendingBooking(owner, 0);
        String key = "key-" + uniqueId;
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger duplicates = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        for (int i = 0; i < 5; i++) {
            executor.execute(() -> {
                try {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(owner.getEmail(), null));
                    startGate.await();
                    ResponseEntity<?> response = paymentService.initiatePayment(booking.getId(), key);
                    if (response.getStatusCode() == HttpStatus.OK) {
                        success.incrementAndGet();
                    }
                } catch (DataIntegrityViolationException e) {
                    duplicates.incrementAndGet();
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
        assertEquals(5, success.get() + duplicates.get(), "every request must either succeed or be rejected as duplicate");
        assertTrue(success.get() >= 1, "at least one request must succeed");
        assertTrue(paymentRepository.findByIdempotencyKey(key).isPresent(), "payment must be saved");
        assertEquals(1, countPaymentsForBooking(booking.getId()), "only one payment row for the idempotency key");
    }

    @Test
    void FiveUsersPayingTheirOwnBookingsAllSucceed() throws Exception {
        List<Booking> bookings = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            bookings.add(createPendingBooking(users.get(i), i));
        }
        long paymentsBefore = paymentRepository.count();
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        for (int i = 0; i < 5; i++) {
            String email = users.get(i).getEmail();
            Long bookingId = bookings.get(i).getId();
            String key = "key-" + uniqueId + "-" + i;
            executor.execute(() -> {
                try {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(email, null));
                    startGate.await();
                    paymentService.initiatePayment(bookingId, key);
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
        assertEquals(5, success.get(), "all five payments must succeed");
        assertEquals(paymentsBefore + 5, paymentRepository.count(), "new five payment rows in the database");
        for (Booking b : bookings) {
            assertEquals(1, countPaymentsForBooking(b.getId()), "each booking has exactly one payment");
        }
    }

    @Test
    void SameWebhookDeliveredFiveTimesConfirmsBookingOnce() throws Exception {
        User owner = users.get(0);
        Booking booking = createPendingBooking(owner, 0);
        String sessionId = "cs_webhook_" + uniqueId;
        createPendingPayment(booking, sessionId);
        String payload = checkoutCompletedPayload(sessionId);
        String signature = signatureHeader(payload);
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        for (int i = 0; i < 5; i++) {
            executor.execute(() -> {
                try {
                    startGate.await();
                    ResponseEntity<?> response = transactionTemplate.execute(
                            status -> paymentService.handleStripeWebhook(payload, signature));
                    if (response != null && response.getStatusCode() == HttpStatus.OK) {
                        accepted.incrementAndGet();
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                }
            });
        }

        startGate.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        Booking updated = bookingRepository.findById(booking.getId()).orElseThrow();
        Payment payment = paymentRepository.findByProviderReference(sessionId);
        assertEquals(0, errors.get(), "unexpected errors");
        assertEquals(5, accepted.get(), "every webhook delivery must be acknowledged");
        assertEquals(Booking.Status.CONFIRMED, updated.getStatus(), "booking must be confirmed");
        assertEquals(Payment.STATUS.SUCCEED, payment.getStatus(), "payment must be succeeded");
        verify(emailService, times(1)).sendBookingConfirmationEmail(any());
        verify(notificationService, times(1)).sendBookingNotification(any());
    }

    @Test
    void WebhookForExpiredHoldDoesNotConfirmBooking() throws Exception {
        User owner = users.get(0);
        Booking booking = createPendingBooking(owner, 0);
        booking.setHoldExpiresAt(LocalDateTime.now().minusMinutes(1));
        bookingRepository.save(booking);
        String sessionId = "cs_expired_" + uniqueId;
        createPendingPayment(booking, sessionId);
        String payload = checkoutCompletedPayload(sessionId);
        String signature = signatureHeader(payload);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger errors = new AtomicInteger();
        for (int i = 0; i < 3; i++) {
            executor.execute(() -> {
                try {
                    startGate.await();
                    transactionTemplate.execute(
                            status -> paymentService.handleStripeWebhook(payload, signature));
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                }
            });
        }
        startGate.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        Booking updated = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals(0, errors.get(), "unexpected errors");
        assertEquals(Booking.Status.PENDING, updated.getStatus(), "expired booking must not be confirmed");
        verify(emailService, never()).sendBookingConfirmationEmail(any());
        verify(notificationService, never()).sendBookingNotification(any());
    }

    @Test
    void CancelAndWebhookAtSameTimeLeaveConsistentState() throws Exception {
        User owner = users.get(0);
        owner.setRole(roleRepository.findByName(Role.RoleName.CUSTOMER).orElseThrow());
        userRepository.save(owner);
        Booking booking = createPendingBooking(owner, 0);
        String sessionId = "cs_cancel_" + uniqueId;
        createPendingPayment(booking, sessionId);
        String payload = checkoutCompletedPayload(sessionId);
        String signature = signatureHeader(payload);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger cancelled = new AtomicInteger();
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        executor.execute(() -> {
            try {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(owner.getEmail(), null));
                startGate.await();
                bookingService.cancelBooking(booking.getId());
                cancelled.incrementAndGet();
            } catch (Exception e) {
                errors.incrementAndGet();
                e.printStackTrace();
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
        for (int i = 0; i < 3; i++) {
            executor.execute(() -> {
                try {
                    startGate.await();
                    ResponseEntity<?> response = transactionTemplate.execute(
                            status -> paymentService.handleStripeWebhook(payload, signature));
                    if (response != null && response.getStatusCode() == HttpStatus.OK) {
                        accepted.incrementAndGet();
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                }
            });
        }
        startGate.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        Booking updated = bookingRepository.findById(booking.getId()).orElseThrow();
        Payment payment = paymentRepository.findByProviderReference(sessionId);
        assertEquals(0, errors.get(), "unexpected errors");
        assertEquals(1, cancelled.get(), "the cancel request must succeed");
        assertEquals(3, accepted.get(), "every webhook delivery must be acknowledged");
        assertEquals(Booking.Status.CANCELLED, updated.getStatus(), "booking must end cancelled");
        assertEquals(Payment.STATUS.SUCCEED, payment.getStatus(), "payment must be recorded as succeeded");
        verify(emailService, times(1)).sendBookingCancellationEmail(any());
        verify(emailService, atMost(1)).sendBookingConfirmationEmail(any());
    }

    private Booking createPendingBooking(User owner, int offset) {
        Booking booking = new Booking();
        booking.setBookingReference("BK-" + uniqueId + "-" + offset);
        booking.setCheckIn(LocalDate.now().plusDays(2 + offset));
        booking.setCheckOut(LocalDate.now().plusDays(3 + offset));
        booking.setAdults(1);
        booking.setChildren(0);
        booking.setTotalAmount(new BigDecimal("100.00"));
        booking.setStatus(Booking.Status.PENDING);
        booking.setHoldExpiresAt(LocalDateTime.now().plusMinutes(15));
        booking.setHotel(hotel);
        booking.setUser(owner);
        return bookingRepository.save(booking);
    }

    private void createPendingPayment(Booking booking, String sessionId) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalAmount());
        payment.setStatus(Payment.STATUS.PENDING);
        payment.setProviderReference(sessionId);
        payment.setIdempotencyKey("key-" + sessionId);
        payment.setSessionURL("https://stripe.test/" + sessionId);
        paymentRepository.save(payment);
    }

    private long countPaymentsForBooking(Long bookingId) {
        return paymentRepository.findAll().stream()
                .filter(p -> p.getBooking().getId().equals(bookingId))
                .count();
    }

    private String checkoutCompletedPayload(String sessionId) {
        return "{"
                + "\"id\":\"evt_" + sessionId + "\","
                + "\"object\":\"event\","
                + "\"api_version\":\"" + Stripe.API_VERSION + "\","
                + "\"type\":\"checkout.session.completed\","
                + "\"data\":{\"object\":{"
                + "\"id\":\"" + sessionId + "\","
                + "\"object\":\"checkout.session\","
                + "\"payment_status\":\"paid\","
                + "\"payment_intent\":\"pi_" + sessionId + "\""
                + "}}}";
    }
    private String signatureHeader(String payload) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        String signature = Webhook.Util.computeHmacSha256(webhookSecret, timestamp + "." + payload);
        return "t=" + timestamp + ",v1=" + signature;
    }
}
