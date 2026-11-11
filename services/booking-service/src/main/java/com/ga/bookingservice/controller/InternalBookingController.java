package com.ga.bookingservice.controller;

import com.ga.bookingservice.exception.custom.UnauthorizedActionException;
import com.ga.bookingservice.service.BookingService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

// endpoints only other services call (never the browser).
// the api-gateway has no route for /internal/**, so it can't be reached from outside,
// and every call must also send the shared X-Internal-Key.
@Hidden
@RestController
@RequestMapping("/internal/bookings")
public class InternalBookingController {
    @Autowired
    private BookingService bookingService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    @PostMapping("/{bookingId}/confirm-payment")
    public ResponseEntity<?> confirmPayment(@PathVariable Long bookingId,
                                            @RequestHeader(value = "X-Internal-Key", required = false) String key) {
        checkKey(key);
        return ResponseEntity.ok(bookingService.confirmPaidBooking(bookingId));
    }

    private void checkKey(String key) {
        // constant-time compare so the key can't be guessed from response timing
        if (key == null || !MessageDigest.isEqual(
                key.getBytes(StandardCharsets.UTF_8), internalApiKey.getBytes(StandardCharsets.UTF_8))) {
            throw new UnauthorizedActionException("Only internal services can call this endpoint");
        }
    }
}
