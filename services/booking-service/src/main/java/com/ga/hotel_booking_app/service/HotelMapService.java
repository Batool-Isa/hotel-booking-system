package com.ga.hotel_booking_app.service;

import com.ga.hotel_booking_app.model.Booking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.concurrent.Semaphore;
@Service
public class HotelMapService {
    private static final java.util.Map<String, byte[]> MAP_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Semaphore semaphore = new Semaphore(2);
    private static final Logger log = LoggerFactory.getLogger(HotelMapService.class);

    public byte[] loadHotelMap(Booking booking) {
        BigDecimal latitude = booking.getHotel().getLatitude();
        BigDecimal longitude = booking.getHotel().getLongitude();
        if (latitude == null || longitude == null) {
            return null;
        }
        String key = latitude.toPlainString() + "," + longitude.toPlainString();
        byte[] cached = MAP_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            semaphore.acquire();
            log.info("{} waiting for permit", Thread.currentThread().getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("{} interrupted ", Thread.currentThread().getName());

            return null;
        }
        try {
            // check if image is already cached
            byte[] image = MAP_CACHE.get(key);
            if (image != null) {
                return image;
            }
            byte[] png = MapImageBuilder.build(latitude.doubleValue(), longitude.doubleValue());
            if (png != null) {
                MAP_CACHE.put(key, png);
            }
            return png;
        } finally {
            semaphore.release();
            log.info("{} release for permit", Thread.currentThread().getName());

        }

    }
}
