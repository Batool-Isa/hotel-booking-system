package com.ga.billingservice.controller;


import com.ga.bookingservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings/{bookingId}/payment")
public class PaymentController {
    @Autowired
    private PaymentService stripeService;

    @PostMapping
    public ResponseEntity<?> initiatePayment(@PathVariable("bookingId") Long id,  @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return stripeService.initiatePayment(id, idempotencyKey);
    }


}
