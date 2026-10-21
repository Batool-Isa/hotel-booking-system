package com.ga.hotel_booking_app.config;

import com.ga.hotel_booking_app.dto.request.PaymentRequest;
import com.ga.hotel_booking_app.dto.response.StripeResponse;
import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    @Value("${stripe.secretKey}")
    private String secretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

}
