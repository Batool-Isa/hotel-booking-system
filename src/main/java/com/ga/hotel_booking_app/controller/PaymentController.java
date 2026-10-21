package com.ga.hotel_booking_app.controller;


import com.ga.hotel_booking_app.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings/{bookingId}/payment")
public class PaymentController {
    @Autowired
    private PaymentService stripeService;
    @PostMapping
    public ResponseEntity<?> initiatePayment(@PathVariable("bookingId") Long id){
        return stripeService.initiatePayment(id);

    }
}
