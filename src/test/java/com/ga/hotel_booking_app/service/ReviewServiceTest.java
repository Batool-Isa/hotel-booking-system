package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.request.ReviewRequest;
import com.ga.hotel_booking_app.exception.custom.InformationExistException;
import com.ga.hotel_booking_app.exception.custom.InvalidInformationException;
import com.ga.hotel_booking_app.exception.custom.UnauthorizedActionException;
import com.ga.hotel_booking_app.model.Booking;
import com.ga.hotel_booking_app.model.Review;
import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.repository.BookingRepository;
import com.ga.hotel_booking_app.repository.ReviewRepository;
import com.ga.hotel_booking_app.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogService auditLogService;
    @InjectMocks
    private ReviewService reviewService;
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

    private Booking booking(User owner, Booking.Status status) {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUser(owner);
        booking.setStatus(status);
        return booking;
    }

    private ReviewRequest reviewRequest() {
        ReviewRequest request = new ReviewRequest();
        ReflectionTestUtils.setField(request, "rating", 4.0);
        ReflectionTestUtils.setField(request, "comment", "Great stay");
        return request;
    }

    @Test
    @DisplayName("Post review for completed booking successfully")
    void postReviewForCompletedBookingShouldSucceed() {
        User customer = loginCustomer(1L);
        Booking booking = booking(customer, Booking.Status.COMPLETED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(false);
        ResponseEntity<?> response = reviewService.postReview(1L, reviewRequest());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        ArgumentCaptor<Review> saved = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(saved.capture());
        assertEquals(4.0, saved.getValue().getRating());
        assertEquals(booking, saved.getValue().getBooking());
    }

    @Test
    @DisplayName("Post review for uncompleted booking throws InvalidInformationException")
    void postReviewForUncompletedBookingShouldThrowsInvalidInformationException() {
        User customer = loginCustomer(1L);
        Booking booking = booking(customer, Booking.Status.CONFIRMED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(false);
        assertThrows(InvalidInformationException.class, () -> reviewService.postReview(1L, reviewRequest()));
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Post second review for same booking throws InformationExistException")
    void postDuplicateReviewShouldThrowsInformationExistException() {
        User customer = new User();
        customer.setId(1L);
        Booking booking = booking(customer, Booking.Status.COMPLETED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(true);
        assertThrows(InformationExistException.class, () -> reviewService.postReview(1L, reviewRequest()));
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Customer cannot review another customer's booking")
    void customerCannotReviewAnotherUsersBooking() {
        User owner = new User();
        owner.setId(1L);
        Booking booking = booking(owner, Booking.Status.COMPLETED);
        loginCustomer(2L);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(false);
        assertThrows(UnauthorizedActionException.class, () -> reviewService.postReview(1L, reviewRequest()));
        verify(reviewRepository, never()).save(any(Review.class));
    }
}
