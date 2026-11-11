package com.ga.bookingservice.controller;

import com.ga.bookingservice.dto.request.BookingRequest;
import com.ga.bookingservice.dto.request.BookingStatusRequest;
import com.ga.bookingservice.model.Booking;
import com.ga.bookingservice.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    @Autowired
    private BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create new booking", description = "Validate user request and insert new booking")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request :Invalid information"),
            @ApiResponse(responseCode = "403", description = "Only customer can create a booking"),
            @ApiResponse(responseCode = "404", description = "Selected room is not found")
    })
    public ResponseEntity<?> createNewBooking(@Valid @RequestBody BookingRequest request){
        return bookingService.createNewBooking(request);
    }
    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get Customer's booking", description = "Fetch all customer's booking records")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Get Logged in user's booking"),
    })
    public ResponseEntity<?> getMyBookings(Pageable pageable){
        return bookingService.getMyBookings(pageable);
    }
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all app's booking",
            description = "Every booking in the app. Optional filters: hotelId, status, "
                    + "when (upcoming | current | past) and q (booking reference)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of bookings"),
    })
    public ResponseEntity<?> getAllAppBookings(@RequestParam(required = false) Long hotelId,
                                               @RequestParam(required = false) Booking.Status status,
                                               @RequestParam(required = false) String when,
                                               @RequestParam(required = false) String q,
                                               Pageable pageable){
        return bookingService.getAllAppBookings(hotelId, status, when, q, pageable);
    }

    @GetMapping("/managed")
    @PreAuthorize("hasRole('HOTEL_MANAGER')")
    @Operation(summary = "Get manager's bookings",
            description = "List all bookings across the hotels the logged-in manager runs")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of bookings"),
            @ApiResponse(responseCode = "403", description = "The hotel is not managed by this user"),
    })
    public ResponseEntity<?> getManagerBookings(@RequestParam(required = false) Long hotelId,
                                                @RequestParam(required = false) Booking.Status status,
                                                @RequestParam(required = false) String when,
                                                @RequestParam(required = false) String q,
                                                Pageable pageable) {
        return bookingService.getManagerBookings(hotelId, status, when, q, pageable);
    }

    @PatchMapping("/{bookingId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'HOTEL_MANAGER')")
    @Operation(summary = "Cancel Booking", description = "Cancel booking")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking canceled successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request :Invalid information"),
            @ApiResponse(responseCode = "404", description = "Booking is not found"),
            @ApiResponse(responseCode = "403", description = "Customer can only cancel their booking & manager can only cancel booking in their own hotels")

    })
    public ResponseEntity<?> cancelBooking(@PathVariable Long bookingId) {
        return bookingService.cancelBooking(bookingId);

    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'HOTEL_MANAGER')")
    @Operation(summary = "Get booking by Id", description = "Fetch booking by Id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "View Booking details"),
            @ApiResponse(responseCode = "404", description = "Booking is not found"),
            @ApiResponse(responseCode = "403", description = "Customer can only view their booking & manager can only view booking in their own hotels")

    })
    public ResponseEntity<?> getBookingById(@PathVariable Long bookingId) {
        return bookingService.getBookingById(bookingId);

    }

    @PatchMapping("/{bookingId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_MANAGER')")
    @Operation(summary = "Update booking status", description = "Update booking status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Update Booking status"),
            @ApiResponse(responseCode = "404", description = "Booking is not found"),
            @ApiResponse(responseCode = "400", description = "Bad request :Invalid status update"),
            @ApiResponse(responseCode = "403", description = "Manager can only update booking in their own hotels")

    })
    public ResponseEntity<?> updateBookingStatus(@PathVariable Long bookingId,
                                                 @Valid @RequestBody BookingStatusRequest request) {
        return bookingService.updateBookingStatus(bookingId, request);

    }

}
