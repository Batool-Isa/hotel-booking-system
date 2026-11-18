package com.ga.billingservice.cucumber;

import com.ga.billingservice.client.BookingServiceClient;
import com.ga.billingservice.kafka.BillingEventProducer;
import com.ga.billingservice.service.StripeGateway;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
public class CucumberSpringConfiguration {
    @MockitoBean
    BookingServiceClient bookingServiceClient;
    @MockitoBean
    StripeGateway stripeGateway;
    @MockitoBean
    BillingEventProducer billingEventProducer;
}
