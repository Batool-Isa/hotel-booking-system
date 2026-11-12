package com.ga.bookingservice.event;

import com.ga.bookingservice.kafka.BookingEventProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class BookingEventRelay {
    private final BookingEventProducer producer;
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookingChanged(BookingChanged changed) {

        producer.publish(changed.booking(), changed.type());
    }
}
