package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.BookingNotification;
import com.ga.hotel_booking_app.model.Booking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    public void sendBookingNotification(Booking booking) {
        String message;

        if (booking.getStatus() == Booking.Status.CONFIRMED) {
            message = "Booking confirmed successfully";
        } else if (booking.getStatus() == Booking.Status.CANCELLED) {
            message = "Booking has been cancelled";
        } else {
            message = "Booking status updated";
        }
        BookingNotification notification = new BookingNotification(
                booking.getBookingReference(),
                message,
                booking.getStatus().name()
        );
        System.out.println("Sending WebSocket notification: "
                + booking.getBookingReference());
        simpMessagingTemplate.convertAndSend(
                "/topic/bookings",
                notification
        );
    }


}
