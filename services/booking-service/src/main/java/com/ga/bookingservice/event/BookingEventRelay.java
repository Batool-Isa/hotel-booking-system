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

    // AFTER_COMMIT: runs only if the booking change was really saved. if the transaction rolls back,
    // the signal is dropped, so no "confirmed" message is ever sent for a booking that wasn't saved.
    // fallbackExecution = true: also runs when the caller has no transaction (instead of silently dropping it)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookingChanged(BookingChanged changed) {
        producer.publish(changed.booking(), changed.type());
    }
}
