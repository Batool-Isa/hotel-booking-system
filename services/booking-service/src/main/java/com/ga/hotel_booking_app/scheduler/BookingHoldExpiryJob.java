package com.ga.hotel_booking_app.scheduler;

import com.ga.hotel_booking_app.model.Booking;
import com.ga.hotel_booking_app.repository.BookingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class BookingHoldExpiryJob {
    private static final Logger logger = LoggerFactory.getLogger(BookingHoldExpiryJob.class);
    private final BookingRepository bookingRepository;
    @Scheduled(fixedRate = 60000)
    @Transactional
public void cancelBooking(){
    // get expired booking that was pending and customer did not paid
List<Booking> expiredBookingList = bookingRepository.findByStatusAndHoldExpiresAtBefore(Booking.Status.PENDING, LocalDateTime.now());
for(Booking booking: expiredBookingList){
    booking.setStatus(Booking.Status.CANCELLED);
    logger.info("Booking {} is cancelled because customer did not pay", booking.getId());

}
}

}
