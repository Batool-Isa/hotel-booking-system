package com.ga.bookingservice.kafka;

import com.ga.bookingservice.client.HotelServiceClient;
import com.ga.bookingservice.client.HotelSummary;
import com.ga.bookingservice.client.UserServiceClient;
import com.ga.bookingservice.client.UserSummary;
import com.ga.bookingservice.config.KafkaTopicConfig;
import com.ga.bookingservice.event.BookingEvent;
import com.ga.bookingservice.model.Booking;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class BookingEventProducer {
    private static final Logger logger = LoggerFactory.getLogger(BookingEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final HotelServiceClient hotelServiceClient;
    private final UserServiceClient userServiceClient;

    // called after the booking change is committed (see BookingEventRelay).
    // nothing in here may throw: a notification problem must never undo or fail the real action
    public void publish(Booking booking, String type) {
        try {
            HotelSummary hotel = fetchHotel(booking);
            UserSummary user = fetchUser(booking);
            BookingEvent event = BookingEvent.from(booking, type, hotel, user);
            String json = jsonMapper.writeValueAsString(event);

            // key = bookingId -> all events of one booking go to the same partition, in order
            kafkaTemplate.send(KafkaTopicConfig.BOOKING_TOPIC, String.valueOf(booking.getId()), json)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            logger.error("Could not publish {} for booking {}: {}",
                                    type, booking.getBookingReference(), error.getMessage());
                        } else {
                            logger.info("Published {} for booking {} to partition {}, offset {}",
                                    type, booking.getBookingReference(),
                                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            logger.error("Could not publish {} for booking {}: {}", type, booking.getBookingReference(), e.getMessage());
        }
    }

    // the hotel summary endpoint needs no token
    private HotelSummary fetchHotel(Booking booking) {
        try {
            return hotelServiceClient.getHotelSummary(booking.getHotelId());
        } catch (Exception e) {
            logger.warn("Hotel {} unavailable for booking event: {}", booking.getHotelId(), e.getMessage());
            return null;
        }
    }

    // internal lookup (X-Internal-Key), so it also works with no logged-in user:
    // the Stripe webhook (payment confirmed) and the hold-expiry job
    private UserSummary fetchUser(Booking booking) {
        try {
            return userServiceClient.findUserInternal(booking.getUserId()).orElse(null);
        } catch (Exception e) {
            logger.warn("User {} unavailable for booking event: {}", booking.getUserId(), e.getMessage());
            return null;
        }
    }
}
