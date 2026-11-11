package com.ga.billingservice.controller;


import com.ga.billingservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings/{bookingId}/payment")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> initiatePayment(@PathVariable("bookingId") Long id,
                                             @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return paymentService.initiatePayment(id, idempotencyKey);
    }
}
