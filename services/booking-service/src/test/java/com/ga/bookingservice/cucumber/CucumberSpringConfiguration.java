package com.ga.bookingservice.cucumber;

import com.ga.bookingservice.client.HotelServiceClient;
import com.ga.bookingservice.client.UserServiceClient;
import com.ga.bookingservice.kafka.AuditLogEventProducer;
import com.ga.bookingservice.kafka.BookingEventProducer;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CucumberSpringConfiguration {
    @MockitoBean
    HotelServiceClient hotelServiceClient;
    @MockitoBean
    UserServiceClient userServiceClient;
    @MockitoBean
    BookingEventProducer bookingEventProducer;
    @MockitoBean
    AuditLogEventProducer auditEventProducer;
//    @MockitoBean
//    EmailService emailService;
}
