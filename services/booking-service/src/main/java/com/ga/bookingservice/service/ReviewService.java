package com.ga.bookingservice.service;

import com.ga.bookingservice.client.UserServiceClient;
import com.ga.bookingservice.client.UserSummary;
import com.ga.bookingservice.dto.request.ReviewRequest;
import com.ga.bookingservice.dto.response.HotelRatingResponse;
import com.ga.bookingservice.dto.response.HotelRatingSummaryResponse;
import com.ga.bookingservice.dto.response.MessageResponse;
import com.ga.bookingservice.dto.response.ReviewResponse;
import com.ga.bookingservice.exception.custom.InformationExistException;
import com.ga.bookingservice.exception.custom.InformationNotFoundException;
import com.ga.bookingservice.exception.custom.InvalidInformationException;
import com.ga.bookingservice.exception.custom.UnauthorizedActionException;
import com.ga.bookingservice.model.Booking;
import com.ga.bookingservice.model.Review;
import com.ga.bookingservice.repository.BookingRepository;
import com.ga.bookingservice.repository.ReviewRepository;
import com.ga.bookingservice.security.AuthUser;
import com.ga.bookingservice.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private CurrentUser currentUser;
    @Autowired
    private AuditLogService auditLogService;
    @Autowired
    private UserServiceClient userServiceClient;

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
        AuthUser user = currentUser.get();
        if(!user.getId().equals(booking.getUserId())){
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
        UserSummary guest = userServiceClient.findUserInternal(user.getId()).orElse(null);
        auditLogService.log(user.getId(), "POST_REVIEW", "Review", review.getId(),
                "User "+ (guest != null ? guest.getUsername():null)+" posted review successfully"
        );
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
            UserSummary guest = userServiceClient.findUserInternal(r.getBooking().getUserId()).orElse(null);
            response1.setGuestName(guest != null ? guest.getUsername() : null);
            response1.setRating(r.getRating());
            reviewResponses.add(response1);
        }
        response.setAverageRating(averageRating);
        response.setReviews(reviewResponses);
        return ResponseEntity.ok(response);

    }

    public ResponseEntity<?> getHotelRating(Long hotelId) {
        List<Review> reviewList = reviewRepository.findHotelReviews(hotelId);
        double averageRating = reviewList.stream().mapToDouble(Review::getRating).average().orElse(0);
        return ResponseEntity.ok(new HotelRatingSummaryResponse(averageRating, reviewList.size()));
    }
}
