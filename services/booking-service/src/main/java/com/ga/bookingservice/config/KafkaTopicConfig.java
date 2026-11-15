package com.ga.bookingservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.stereotype.Component;

@Component
public class KafkaTopicConfig {
    public static final String BOOKING_TOPIC = "booking-events";
    public static final String AUDIT_TOPIC = "audit-log-events";

    @Bean
    public NewTopic bookingEventsTopic() {
        return TopicBuilder.name(BOOKING_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
    @Bean
    public NewTopic auditEventsTopic(){
        return TopicBuilder.name(AUDIT_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
