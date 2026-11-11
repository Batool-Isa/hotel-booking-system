package com.ga.billingservice.client;
import com.ga.billingservice.exception.custom.ServiceUnavailableException;
import com.ga.billingservice.exception.custom.UnauthorizedActionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Optional;

@Component
public class BookingServiceClient {
    private final RestClient restClient;
    private final String internalApiKey;

    public BookingServiceClient(@Value("${services.booking.url}") String url,
                                @Value("${internal.api-key}") String internalApiKey) {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(Duration.ofSeconds(2));
        clientHttpRequestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder().baseUrl(url).requestFactory(clientHttpRequestFactory).build();
        this.internalApiKey = internalApiKey;
    }

    public Optional<BookingSummary> getBooking(Long bookingId, String token) {
        try {
            BookingSummary response = restClient.get()
                    .uri("/api/bookings/{bookingId}", bookingId)
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .retrieve()
                    .body(BookingSummary.class);
            return Optional.ofNullable(response);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.Unauthorized e) {
            throw new UnauthorizedActionException("You are not allowed to pay for booking " + bookingId);
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Booking service is not available right now. Please try again.");
        }
    }
    public Optional<BookingSummary> confirmPayment(Long bookingId) {
        try {
            BookingSummary response = restClient.post()
                    .uri("/internal/bookings/{bookingId}/confirm-payment", bookingId)
                    .header("X-Internal-Key", internalApiKey)
                    .retrieve()
                    .body(BookingSummary.class);
            return Optional.ofNullable(response);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Booking service is not available right now. Please try again.");
        }
    }
}
