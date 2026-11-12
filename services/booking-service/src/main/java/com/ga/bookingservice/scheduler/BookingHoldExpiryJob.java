package com.ga.bookingservice.scheduler;

import com.ga.bookingservice.event.BookingChanged;
import com.ga.bookingservice.model.Booking;
import com.ga.bookingservice.repository.BookingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class BookingHoldExpiryJob {
    private static final Logger logger = LoggerFactory.getLogger(BookingHoldExpiryJob.class);
    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelBooking() {
        // get expired booking that was pending and customer did not paid
        List<Booking> expiredBookingList = bookingRepository.findByStatusAndHoldExpiresAtBefore(Booking.Status.PENDING, LocalDateTime.now());
        for (Booking booking : expiredBookingList) {
            booking.setStatus(Booking.Status.CANCELLED);
            logger.info("Booking {} is cancelled because customer did not pay", booking.getId());
            // sent to Kafka after this job's transaction commits (no user token here: the event
            // producer looks the guest up through user-service's internal endpoint)
            applicationEventPublisher.publishEvent(new BookingChanged(booking, "BOOKING_EXPIRED"));

        }
    }

}
