package com.ga.bookingservice.event;

import com.ga.bookingservice.model.Booking;

// in-memory "something happened to this booking" signal (Spring's ApplicationEventPublisher, NOT Kafka).
// BookingService announces it inside the transaction; BookingEventRelay turns it into a Kafka message
// only after the transaction has committed.
public record BookingChanged(Booking booking, String type) {
}
