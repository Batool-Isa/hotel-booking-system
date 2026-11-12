package com.ga.notificationservice.kafka;

import com.ga.notificationservice.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(PaymentEventConsumer.class);
    private final JsonMapper jsonMapper;

    @KafkaListener(topics = "payment-events", groupId = "notification-service")
    public void onPaymentEvent(String json,
                               @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                               @Header(KafkaHeaders.OFFSET) long offset) {
        PaymentEvent event = jsonMapper.readValue(json, PaymentEvent.class);
        logger.info("Received {} for payment {} (booking {}, guest {}, amount {} {}) [partition {}, offset {}]",
                event.type(), event.paymentId(), event.bookingReference(), event.guestEmail(),
                event.amount(), event.currency(), partition, offset);

        switch (event.type()) {
            case "PAYMENT_SUCCEEDED" -> logger.info("-> would send: payment receipt");
            case "PAYMENT_FAILED" -> logger.info("-> would send: payment did not go through, try again");
            case "PAYMENT_LATE" -> logger.info("-> would send: admin alert, paid for a cancelled booking, refund needed");
            default -> logger.warn("Unknown payment event type {}", event.type());
        }
    }
}
