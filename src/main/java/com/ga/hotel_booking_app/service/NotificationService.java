package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.BookingNotification;
import com.ga.hotel_booking_app.model.Booking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;
    private static final Logger logger =
            LoggerFactory.getLogger(NotificationService.class);
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
        logger.info("Sending WebSocket notification: {}", booking.getBookingReference());
        simpMessagingTemplate.convertAndSend(
                "/topic/bookings",
                notification
        );
    }


}
