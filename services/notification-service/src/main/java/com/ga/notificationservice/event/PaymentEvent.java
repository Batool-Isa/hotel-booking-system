package com.ga.notificationservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentEvent(
        String eventId,
        String type,
        Long paymentId,
        Long bookingId,
        String bookingReference,
        Long userId,
        String guestName,
        String guestEmail,
        BigDecimal amount,
        String currency,
        String paymentIntent,
        LocalDateTime occurredAt
) {
}
