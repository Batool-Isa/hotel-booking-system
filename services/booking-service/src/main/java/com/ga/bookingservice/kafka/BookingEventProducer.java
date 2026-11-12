package com.ga.bookingservice.kafka;

import com.ga.bookingservice.client.HotelServiceClient;
import com.ga.bookingservice.client.HotelSummary;
import com.ga.bookingservice.client.UserServiceClient;
import com.ga.bookingservice.client.UserSummary;
import com.ga.bookingservice.config.KafkaTopicConfig;
import com.ga.bookingservice.event.BookingEvent;
import com.ga.bookingservice.model.Booking;
import com.ga.bookingservice.repository.BookingRepository;
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
    private final BookingRepository bookingRepository;


    public void publish(Booking booking, String type) {
        try {
            Booking loaded = reloadWithRooms(booking);
            HotelSummary hotel = fetchHotel(loaded);
            UserSummary user = fetchUser(loaded);
            BookingEvent event = BookingEvent.from(loaded, type, hotel, user);
            String json = jsonMapper.writeValueAsString(event);

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

    private Booking reloadWithRooms(Booking booking) {
        try {
            return bookingRepository.findWithRoomsById(booking.getId()).orElse(booking);
        } catch (Exception e) {
            logger.warn("Could not reload booking {} with rooms: {}", booking.getId(), e.getMessage());
            return booking;
        }
    }

    private HotelSummary fetchHotel(Booking booking) {
        try {
            return hotelServiceClient.getHotelSummary(booking.getHotelId());
        } catch (Exception e) {
            logger.warn("Hotel {} unavailable for booking event: {}", booking.getHotelId(), e.getMessage());
            return null;
        }
    }


    private UserSummary fetchUser(Booking booking) {
        try {
            return userServiceClient.findUserInternal(booking.getUserId()).orElse(null);
        } catch (Exception e) {
            logger.warn("User {} unavailable for booking event: {}", booking.getUserId(), e.getMessage());
            return null;
        }
    }
}
