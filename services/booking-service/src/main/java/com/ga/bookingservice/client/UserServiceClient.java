package com.ga.bookingservice.client;


import com.ga.bookingservice.exception.custom.ServiceUnavailableException;
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
public class UserServiceClient {
    private final RestClient restClient;
    private final String internalApiKey;

    public UserServiceClient(@Value("${services.user.url}") String url,
                             @Value("${internal.api-key}") String internalApiKey) {
        this.internalApiKey = internalApiKey;

        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(Duration.ofSeconds(2));
        clientHttpRequestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient =RestClient.builder().baseUrl(url).requestFactory(clientHttpRequestFactory).build();
    }

    // service-to-service lookup: works without a user token (Stripe webhook, scheduled jobs).
    // used to build Kafka events. user-service checks the shared X-Internal-Key instead of a JWT
    public Optional<UserSummary> findUserInternal(Long userId) {
        try {
            UserSummary response = restClient.get()
                    .uri("/internal/users/{id}", userId)
                    .header("X-Internal-Key", internalApiKey)
                    .retrieve()
                    .body(UserSummary.class);
            return Optional.ofNullable(response);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("User service is not available right now. Please try again.");
        }
    }

    public Optional<UserSummary> findUser(Long userId, String token) {
        try{
            UserSummary response = restClient.get()
                    .uri("/auth/users/{id}", userId)
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .retrieve()
                    .body(UserSummary.class);
            return Optional.ofNullable(response);
        } catch (HttpClientErrorException.NotFound e) {
           return Optional.empty();
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("User service is not available right now. Please try again.");
        }
    }


    }
