package com.ga.userservice.client;

import com.ga.userservice.exception.custom.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Component
public class StatsClient {
    private final RestClient bookingClient;
    private final RestClient hotelClient;
    private final String internalApiKey;

    public StatsClient(@Value("${services.booking.url}") String bookingUrl,
                       @Value("${services.hotel.url}") String hotelUrl,
                       @Value("${internal.api-key}") String internalApiKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.bookingClient = RestClient.builder().baseUrl(bookingUrl).requestFactory(requestFactory).build();
        this.hotelClient = RestClient.builder().baseUrl(hotelUrl).requestFactory(requestFactory).build();
        this.internalApiKey = internalApiKey;
    }

    public BookingStats getBookingStats() {
        try {
            return bookingClient.get()
                    .uri("/internal/stats")
                    .header("X-Internal-Key", internalApiKey)
                    .retrieve()
                    .body(BookingStats.class);
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Booking service is not available right now, statistics can't be loaded.");
        }
    }

    public HotelStats getHotelStats() {
        try {
            return hotelClient.get()
                    .uri("/internal/stats")
                    .header("X-Internal-Key", internalApiKey)
                    .retrieve()
                    .body(HotelStats.class);
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Hotel service is not available right now, statistics can't be loaded.");
        }
    }
}
