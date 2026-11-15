package com.ga.hotelservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    public static final String AUDIT_TOPIC = "audit-log-events";

    @Bean
    public NewTopic auditLogEvent(){
     return TopicBuilder.name(AUDIT_TOPIC)
             .partitions(3)
             .replicas(1)
             .build();
    }
}
