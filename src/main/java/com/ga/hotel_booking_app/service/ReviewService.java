package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.request.ReviewRequest;
import com.ga.hotel_booking_app.dto.response.HotelRatingResponse;
import com.ga.hotel_booking_app.dto.response.MessageResponse;
import com.ga.hotel_booking_app.dto.response.ReviewResponse;
import com.ga.hotel_booking_app.exception.custom.InformationExistException;
import com.ga.hotel_booking_app.exception.custom.InformationNotFoundException;
import com.ga.hotel_booking_app.exception.custom.InvalidInformationException;
import com.ga.hotel_booking_app.exception.custom.UnauthorizedActionException;
import com.ga.hotel_booking_app.model.Booking;
import com.ga.hotel_booking_app.model.Review;
import com.ga.hotel_booking_app.model.User;
import com.ga.hotel_booking_app.repository.BookingRepository;
import com.ga.hotel_booking_app.repository.ReviewRepository;
import com.ga.hotel_booking_app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReviewService {

    @Autowired
    BookingRepository bookingRepository;
    @Autowired
    ReviewRepository reviewRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuditLogService auditLogService;

    public User getCurrentLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findUserByEmail(email);
    }

    public ResponseEntity<?> postReview(Long bookingId, ReviewRequest request) {
        // check if booking is completed , otherwise review is forbidden
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new InformationNotFoundException("Booking "+bookingId+" not found")
        );
        // check to avoid duplicated review
        if(reviewRepository.existsByBookingId(bookingId)){
            throw new InformationExistException("A review for this booking already posted");
        }
        // check if user owned this booking
        User user = getCurrentLoggedInUser();
        if(!user.getId().equals(booking.getUser().getId())){
            throw new UnauthorizedActionException("You are not authorized to add review to this booking");
        }

        if(!booking.getStatus().equals(Booking.Status.COMPLETED)){
            throw new InvalidInformationException("You can't add review to uncompleted booking");
        }
        Review review = new Review();
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setBooking(booking);
        reviewRepository.save(review);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Review created successfully"));
    }

    public ResponseEntity<?> getHotelReviews(Long hotelId) {
    List<Review> reviewList = reviewRepository.findHotelReviews(hotelId);

    double averageRating = reviewList.stream().mapToDouble(Review::getRating).average().orElse(0);

        HotelRatingResponse response = new HotelRatingResponse();
        List<ReviewResponse> reviewResponses = new ArrayList<>();
        for (Review r : reviewList){
            ReviewResponse response1 = new ReviewResponse();
            response1.setId(r.getId());
            response1.setCreatedAt(r.getCreatedAt());
            response1.setComment(r.getComment());
            response1.setGuestName(r.getBooking().getUser().getUsername());
            response1.setRating(r.getRating());
            reviewResponses.add(response1);
        }
        response.setAverageRating(averageRating);
        response.setReviews(reviewResponses);
        return ResponseEntity.ok(response);

    }
}
