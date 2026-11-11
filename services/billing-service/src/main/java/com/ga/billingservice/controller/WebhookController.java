package com.ga.billingservice.controller;

import com.ga.billingservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
public class WebhookController {
    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<?> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        return paymentService.handleStripeWebhook(payload, sigHeader);
    }
}
