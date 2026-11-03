package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.exception.custom.InvalidInformationException;
import com.ga.hotel_booking_app.exception.custom.UnauthorizedActionException;
import com.ga.hotel_booking_app.model.Booking;
import com.ga.hotel_booking_app.model.Payment;
import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.repository.*;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {
    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private StripeGateway stripeGateway;

    @InjectMocks
    private PaymentService paymentService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private User loginCustomer(Long id) {
        User customer = new User();
        customer.setId(id);
        customer.setEmail("user1@example.com");
        when(userRepository.findUserByEmail("user1@example.com")).thenReturn(customer);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user1@example.com", null)
        );
        return customer;
    }

    private Booking pendingBooking(User owner) {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUser(owner);
        booking.setStatus(Booking.Status.PENDING);
        booking.setHoldExpiresAt(LocalDateTime.now().plusMinutes(15));
        booking.setTotalAmount(new BigDecimal("100.00"));
        booking.setBookingReference("BK-TEST");
        return booking;
    }

    @Test
    @DisplayName("Create & initiate payment session for pending booking")
    void initiatePaymentForPendingBookingShouldSucceed() throws StripeException {
        User customer = loginCustomer(1L);
        Booking booking = pendingBooking(customer);
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("cs_test_123");
        when(session.getUrl()).thenReturn("https://stripe.test/session");

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(stripeGateway.createCheckoutSession(any())).thenReturn(session);
        ResponseEntity<?> response = paymentService.initiatePayment(1L, "key-1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("https://stripe.test/session", response.getBody());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("Create & initiate payment session for pending booking for non owner should throws 403 error")
    void initiatePaymentForPendingBookingForNonOwnerUserShouldThrowsUnauthorizedException()  {
        User customer1 = new User();
        customer1.setId(1L);
        Booking booking = pendingBooking(customer1);
        User customer2 = loginCustomer(2L);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        assertThrows(UnauthorizedActionException.class, () -> paymentService.initiatePayment(1L, "test-key"));
        verifyNoInteractions(stripeGateway);
    }

    @Test
    @DisplayName("Create & initiate payment session for cancelled booking should throws invalid information")
    void initiatePaymentForCancelledShouldThrowsInvalidInformationException()  {
        User customer1 = loginCustomer(1L);
        Booking booking = pendingBooking(customer1);
        booking.setStatus(Booking.Status.CANCELLED);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        assertThrows(InvalidInformationException.class, () -> paymentService.initiatePayment(1L, "test-key"));
        verifyNoInteractions(stripeGateway);
    }

    @Test
    @DisplayName("Reuse The Idempotency Key Should Return Same Session URL")
    void reuseTheIdempotencyKeyShouldReturnSameSessionURL() throws StripeException {
        User customer1 = loginCustomer(1L);
        Booking booking = pendingBooking(customer1);
        Payment existingPayment = new Payment();
        existingPayment.setBooking(booking);
        existingPayment.setIdempotencyKey("key-1");
        existingPayment.setSessionURL("https://stripe.test/existing-session");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existingPayment));
        ResponseEntity<?> response = paymentService.initiatePayment(1L, "key-1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("https://stripe.test/existing-session", response.getBody());
        verify(stripeGateway, never()).createCheckoutSession(any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

}
