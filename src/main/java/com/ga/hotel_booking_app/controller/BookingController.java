package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.dto.request.BookingRequest;
import com.ga.hotel_booking_app.dto.request.BookingStatusRequest;
import com.ga.hotel_booking_app.service.BookingService;
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
        System.out.println("Booking controller calling --> create new booking");
        return bookingService.createNewBooking(request);
    }
    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get Customer's booking", description = "Fetch all customer's booking records")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Get Logged in user's booking"),
    })
    public ResponseEntity<?> getMyBookings(Pageable pageable){
        System.out.println("Booking controller calling --> get my bookings");
        return bookingService.getMyBookings(pageable);
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
        System.out.println("Booking controller calling --> cancel bookings");
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
        System.out.println("Booking controller calling --> get booking by id");
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
        System.out.println("Booking controller calling --> update booking by id");
        return bookingService.updateBookingStatus(bookingId, request);

    }

}
