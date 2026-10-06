package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.dto.request.BookingRequest;
import com.ga.hotel_booking_app.dto.request.BookingStatusRequest;
import com.ga.hotel_booking_app.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    @Autowired
    private BookingService bookingService;

    @PostMapping
    @Operation(summary = "Create new booking", description = "Validate user request and insert new booking")
    public ResponseEntity<?> createNewBooking(@Valid @RequestBody BookingRequest request){
        System.out.println("Booking controller calling --> create new booking");
        return bookingService.createNewBooking(request);
    }
    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get Customer's booking", description = "Fetch all customer's booking records")
    public ResponseEntity<?> getMyBookings(){
        System.out.println("Booking controller calling --> get my bookings");
        return bookingService.getMyBookings();
    }

    @PatchMapping("/{bookingId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'HOTEL_MANAGER')")
    @Operation(summary = "Cancel Booking", description = "Cancel booking")
    public ResponseEntity<?> cancelBooking(@PathVariable Long bookingId) {
        System.out.println("Booking controller calling --> cancel bookings");
        return bookingService.cancelBooking(bookingId);

    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'HOTEL_MANAGER')")
    @Operation(summary = "Get booking by Id", description = "Fetch booking by Id")
    public ResponseEntity<?> getBookingById(@PathVariable Long bookingId) {
        System.out.println("Booking controller calling --> get booking by id");
        return bookingService.getBookingById(bookingId);

    }

    @PatchMapping("/{bookingId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_MANAGER')")
    @Operation(summary = "Get booking by Id", description = "Fetch booking by Id")
    public ResponseEntity<?> updateBookingStatus(@PathVariable Long bookingId,
                                                 @Valid @RequestBody BookingStatusRequest request) {
        System.out.println("Booking controller calling --> update booking by id");
        return bookingService.updateBookingStatus(bookingId, request);

    }

}
