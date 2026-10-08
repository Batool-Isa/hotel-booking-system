package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.dto.BookingNotification;
import com.ga.hotel_booking_app.model.Booking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NotificationService.class);

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
                booking.getStatus().name(),
                booking.getHotel().getId(),
                booking.getHotel().getName()
        );
        System.out.println("Sending WebSocket notification: "
                + booking.getBookingReference());
        // customer who owns the booking
        simpMessagingTemplate.convertAndSendToUser(
                booking.getUser().getEmail(),
                "/queue/bookings",
                notification
        );
        // manager who assign to hotel
        simpMessagingTemplate.convertAndSend(
                "/topic/hotels/" + booking.getHotel().getId() + "/bookings", notification);

        simpMessagingTemplate.convertAndSend("/topic/admin/bookings", notification);
        log.info("WebSocket notification sent for booking {}", booking.getBookingReference());

    }


}
