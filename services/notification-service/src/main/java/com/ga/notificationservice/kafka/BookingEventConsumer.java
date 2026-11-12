package com.ga.notificationservice.kafka;

import com.ga.notificationservice.event.BookingEvent;
import com.ga.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class BookingEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(BookingEventConsumer.class);
    private final JsonMapper jsonMapper;
    private final EmailService emailService;

    @KafkaListener(topics = "booking-events", groupId = "notification-service")
    public void onBookingEvent(String json,
                               @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                               @Header(KafkaHeaders.OFFSET) long offset) {
        BookingEvent event = jsonMapper.readValue(json, BookingEvent.class);
        logger.info("Received {} for booking {} (guest {}, hotel {}, rooms {}) [partition {}, offset {}]",
                event.type(), event.bookingReference(), event.guestEmail(), event.hotelName(),
                event.rooms() == null ? 0 : event.rooms().size(), partition, offset);

        switch (event.type()) {
            case "BOOKING_CONFIRMED" -> emailService.sendBookingConfirmationEmail(event);
            case "BOOKING_CANCELLED", "BOOKING_EXPIRED" -> emailService.sendBookingCancellationEmail(event);
            case "BOOKING_CREATED", "BOOKING_COMPLETED" -> logger.debug("No email for {}", event.type());
            default -> logger.warn("Unknown booking event type {}", event.type());
        }
    }
}
