package com.ga.bookingservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.stereotype.Component;

@Component
public class KafkaTopicConfig {
    public static final String BOOKING_TOPIC = "booking-events";
    @Bean
    public NewTopic bookingEventsTopic() {
        return TopicBuilder.name(BOOKING_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
