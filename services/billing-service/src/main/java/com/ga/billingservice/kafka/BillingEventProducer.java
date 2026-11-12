package com.ga.billingservice.kafka;

import com.ga.billingservice.client.BookingSummary;
import com.ga.billingservice.client.UserServiceClient;
import com.ga.billingservice.client.UserSummary;
import com.ga.billingservice.config.KafkaTopicConfig;
import com.ga.billingservice.event.BillingEvent;
import com.ga.billingservice.model.Payment;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class BillingEventProducer {
    private static final Logger logger = LoggerFactory.getLogger(BillingEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final UserServiceClient userServiceClient;

    public void publish(Payment payment, BookingSummary booking, String type, String customerEmail) {
        try {
            UserSummary user = fetchUser(booking);
            BillingEvent event = BillingEvent.from(payment, booking, type, user, customerEmail);
            String json = jsonMapper.writeValueAsString(event);

            kafkaTemplate.send(KafkaTopicConfig.PAYMENT_TOPIC, String.valueOf(payment.getBookingId()), json)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            logger.error("Could not publish {} for payment {}: {}",
                                    type, payment.getId(), error.getMessage());
                        } else {
                            logger.info("Published {} for payment {} (booking {}) to partition {}, offset {}",
                                    type, payment.getId(), payment.getBookingId(),
                                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            logger.error("Could not publish {} for payment {}: {}", type, payment.getId(), e.getMessage());
        }
    }

    private UserSummary fetchUser(BookingSummary booking) {
        if (booking == null || booking.getUserId() == null) {
            return null;
        }
        try {
            return userServiceClient.findUserInternal(booking.getUserId()).orElse(null);
        } catch (Exception e) {
            logger.warn("User {} unavailable for payment event: {}", booking.getUserId(), e.getMessage());
            return null;
        }
    }
}
