package com.ga.billingservice.event;

import com.ga.billingservice.client.BookingSummary;
import com.ga.billingservice.client.UserSummary;
import com.ga.billingservice.model.Payment;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BillingEvent {
    private String eventId;
    private String type;
    private Long paymentId;
    private Long bookingId;
    private String bookingReference;
    private Long userId;
    private String guestName;
    private String guestEmail;
    private BigDecimal amount;
    private String currency;
    private String paymentIntent;
    private LocalDateTime occurredAt;

    public static BillingEvent from(Payment payment, BookingSummary booking, String type,
                                    UserSummary user, String customerEmail) {
        BillingEvent e = new BillingEvent();
        e.setEventId(UUID.randomUUID().toString());
        e.setType(type);
        e.setPaymentId(payment.getId());
        e.setBookingId(payment.getBookingId());
        e.setBookingReference(booking == null ? null : booking.getBookingReference());
        e.setUserId(booking == null ? null : booking.getUserId());
        e.setGuestName(user == null ? null : user.getFullName());
        e.setGuestEmail(user != null && user.getEmail() != null ? user.getEmail() : customerEmail);
        e.setAmount(payment.getAmount());
        e.setCurrency("usd");
        e.setPaymentIntent(payment.getPaymentIntent());
        e.setOccurredAt(LocalDateTime.now());
        return e;
    }
}
