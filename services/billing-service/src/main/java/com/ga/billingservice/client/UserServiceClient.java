package com.ga.billingservice.client;


import com.ga.billingservice.exception.custom.ServiceUnavailableException;
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
