package com.ga.notificationservice.kafka;

import com.ga.notificationservice.event.BookingEvent;
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

    @KafkaListener(topics = "booking-events", groupId = "notification-service")
    public void onBookingEvent(String json,
                               @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                               @Header(KafkaHeaders.OFFSET) long offset) {
        BookingEvent event = jsonMapper.readValue(json, BookingEvent.class);   // JSON text -> object
        logger.info("Received {} for booking {} (guest {}, hotel {}) [partition {}, offset {}]",
                event.type(), event.bookingReference(), event.guestEmail(), event.hotelName(), partition, offset);

        switch (event.type()) {
            case "BOOKING_CREATED" -> logger.info("-> would send: complete your payment");
            case "BOOKING_CONFIRMED" -> logger.info("-> would send: booking confirmed");
            case "BOOKING_CANCELLED", "BOOKING_EXPIRED" -> logger.info("-> would send: booking cancelled");
            case "BOOKING_COMPLETED" -> logger.info("-> would send: rate your stay");
            default -> logger.warn("Unknown booking event type {}", event.type());
        }
    }
}
