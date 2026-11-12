package com.ga.notificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

@Service
public class HotelMapService {
    private static final Map<String, byte[]> MAP_CACHE = new ConcurrentHashMap<>();
    private static final Semaphore semaphore = new Semaphore(2);
    private static final Logger log = LoggerFactory.getLogger(HotelMapService.class);

    public byte[] loadHotelMap(BigDecimal latitude, BigDecimal longitude) {
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("{} interrupted while waiting for a map permit", Thread.currentThread().getName());
            return null;
        }
        try {
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
        }
    }
}
