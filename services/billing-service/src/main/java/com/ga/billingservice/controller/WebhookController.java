package com.ga.billingservice.controller;


import com.ga.bookingservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
public class WebhookController {
    @Autowired
    private PaymentService stripeService;

    @PostMapping
    public ResponseEntity<?> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        return stripeService.handleStripeWebhook(payload, sigHeader);
    }
}
