package com.ga.hotel_booking_app.controller;

import com.ga.hotel_booking_app.dto.request.BookingRequest;
import com.ga.hotel_booking_app.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    @Autowired
    private BookingService bookingService;

    @PostMapping
    public ResponseEntity<?> createNewBooking(@Valid @RequestBody BookingRequest request){
        System.out.println("Booking controller calling --> create new booking");
        return bookingService.createNewBooking(request);
    }

}
